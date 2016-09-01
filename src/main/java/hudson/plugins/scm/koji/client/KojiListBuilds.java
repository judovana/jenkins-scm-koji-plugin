package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.plugins.scm.koji.BuildsSerializer;
import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.plugins.scm.koji.model.RPM;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.AtomicParser;
import org.apache.xmlrpc.parser.ObjectArrayParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.I4Serializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.jenkinsci.remoting.RoleChecker;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static hudson.plugins.scm.koji.Constants.BUILD_XML;
import static hudson.plugins.scm.koji.Constants.arch;
import static hudson.plugins.scm.koji.KojiSCM.DESCRIPTOR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KojiListBuilds implements FilePath.FileCallable<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(KojiListBuilds.class);

    private final KojiScmConfig config;
    private final GlobPredicate tagPredicate;
    private final Predicate<String> notProcessedNvrPredicate;

    public KojiListBuilds(KojiScmConfig config, Predicate<String> notProcessedNvrPredicate) {
        this.config = config;
        this.tagPredicate = new GlobPredicate(config.getTag());
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
    }

    @Override
    public Build invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Stream<Build> results = listMatchingBuilds();

        Optional<Build> buildOpt = results
                .filter(b -> notProcessedNvrPredicate.test(b.getNvr()))
                .findFirst();
        if (buildOpt.isPresent()) {
            Build build = buildOpt.get();
            LOG.info("oldest not processed build: " + build.getNvr());
            if (!DESCRIPTOR.getKojiSCMConfig()) {
                // do NOT save save BUILD_XML in no-worksapce mode. By creating it, you will  cause the ater pooling to fail
                // and most suprisingly  - NVR get comelty lost
                // I dont know what exactly is causing the lsot of NVRE, but following NPEs missing builds, even not  called koiscm.checkout ...
                // ..fatality. See the rest of "I have no idea what I have done" commit
                // and good new at the end. The  file is writtne later, to workspace anyway....
            } else {
                new BuildsSerializer().write(build, new File(workspace, BUILD_XML));
            }
            return build;
        }
        return null;
    }

    private Stream<Build> listMatchingBuilds() {
        Integer packageId = (Integer) execute(Constants.getPackageID, config.getPackageName());

        Map paramsMap = new HashMap();
        paramsMap.put(Constants.packageID, packageId);
        paramsMap.put("state", 1);
        paramsMap.put("__starstar", Boolean.TRUE);

        Object[] results = (Object[]) execute(Constants.listBuilds, paramsMap);
        if (results == null || results.length < 1) {
            return Stream.empty();
        }
        // ok, obvious over-engineering here:
        return Arrays
                .stream(results)
                .sequential()
                // sorting first, to go with relevant results first:
                .sorted(this::compareBuilds)
                // getting tags per build and filtering by tags right away:
                .map(this::retrieveTags)
                .filter(this::filterByTags)
                // getting rpms and filtering by arch right away:
                .map(this::retrieveRPMs)
                .map(this::retrieveArchives)
                .filter(this::filterByArch)
                // do not go too far away into the past:
                .limit(config.getMaxPreviousBuilds())
                // composing final stream of builds:
                .map(this::toBuild)
                // sorting in reverse order:
                .sorted(Comparator.reverseOrder());
    }

    private Object retrieveTags(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }

        Map m = (Map) o;
        //Object buildName = m.get(nvr);

        Map paramsMap = new HashMap();
        paramsMap.put(Constants.build, m.get(Constants.build_id));
        paramsMap.put("__starstar", Boolean.TRUE);

        Object res = execute(Constants.listTags, paramsMap);
        if (res != null && (res instanceof Object[]) && ((Object[]) res).length > 0) {
            m.put("tags", res);
        }
        return o;
    }

    private boolean filterByTags(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;
        Object buildName = m.get(Constants.name);
        Object[] tags = (Object[]) m.get("tags");
        if (tags == null) {
            return false;
        }
        boolean tagMatch = Arrays
                .stream(tags)
                .map(t -> ((Map<String, String>) t).get(Constants.name))
                .anyMatch(tagPredicate);
        return tagMatch;
    }

    private Object retrieveRPMs(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;

        Map paramsMap = new HashMap();
        paramsMap.put(Constants.buildID, m.get(Constants.build_id));
        String[] arches = composeArchesArray();
        if (arches != null) {
            paramsMap.put(Constants.arches, arches);
        }
        paramsMap.put("__starstar", Boolean.TRUE);

        Object res = execute(Constants.listRPMs, paramsMap);
        if (res != null && (res instanceof Object[]) && ((Object[]) res).length > 0) {
            m.put(Constants.rpms, res);
        }

        return o;
    }

    /**
     * Archives are stored under {@link Constants#rpms} key, together with RPMs.
     * <p>
     * Name, Version and Release are not received with info about archive. We need to get it from the build. Arch is
     * taken from configuration and is later used to compose filepath. Unlike with RPMs, filename is received here so
     * we can store it.
     */
    private Object retrieveArchives(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;

        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put(Constants.buildID, m.get(Constants.build_id));
        String[] arches = composeArchesArray();
        paramsMap.put("__starstar", Boolean.TRUE);

        Object res = execute(Constants.listArchives, paramsMap);
        if (res != null && (res instanceof Object[]) && ((Object[]) res).length > 0) {
            for (Object r : (Object[]) res) {
                if (r != null && r instanceof Map) {
                    for (String arch : arches) {
                        Map<String, Object> rpms = (Map) r;
                        rpms.put(Constants.name, m.get(Constants.name));
                        rpms.put(Constants.version, m.get(Constants.version));
                        rpms.put(Constants.release, m.get(Constants.release));
                        rpms.put(Constants.arch, arch);
                        rpms.put(Constants.nvr, ((Map) r).get(Constants.filename));
                        rpms.put(Constants.filename, ((Map) r).get(Constants.filename));
                    }
                }
            }

            m.put(Constants.rpms, res);
        }

        return o;
    }

    private boolean filterByArch(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        String arch = config.getArch();
        if (arch == null || arch.isEmpty()) {
            throw new RuntimeException("Arch cannot be empty");
        }
        Map m = (Map) o;
        boolean hasRpms = m.get(Constants.rpms) != null;
        return hasRpms;
    }

    private Build toBuild(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }

        Map<String, String> m = (Map) o;
        return new Build((Integer) ((Map) o).get(Constants.build_id),
                m.get(Constants.name),
                m.get(Constants.version),
                m.get(Constants.release),
                m.get(Constants.nvr),
                dateKojiToIso(m.get(Constants.completion_time)),
                Arrays
                        .stream((Object[]) ((Map) o).get(Constants.rpms))
                        .map(this::toRPM)
                        .collect(Collectors.toList()),
                Arrays
                        .stream((Object[]) ((Map) o).get("tags"))
                        .map(t -> ((Map<String, String>) t).get(Constants.name))
                        .collect(Collectors.toSet()), null
        );
    }

    private RPM toRPM(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map<String, String> m = (Map) o;
        return new RPM(
                m.get(Constants.name),
                m.get(Constants.version),
                m.get(Constants.release),
                m.get(Constants.nvr),
                m.get(Constants.arch),
                m.get(Constants.filename));
    }

    private String dateKojiToIso(String kojiDate) {
        LocalDateTime date = LocalDateTime.parse(kojiDate, Constants.DTF);
        return DateTimeFormatter.ISO_DATE_TIME.format(date);
    }

    private int compareBuilds(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            // o2 is not null:
            return 1;
        }
        // o1 is not null:
        if (o2 == null) {
            return -1;
        }
        // both o1 and o2 are not null:
        if (!(o1 instanceof Map) || !(o2 instanceof Map)) {
            throw new RuntimeException("Expected Map instances, got: " + o1 + " and " + o2);
        }
        Map<String, String> m1 = (Map) o1;
        Map<String, String> m2 = (Map) o2;

        // comparing versions:
        int res = compareStrings(m1.get(Constants.version), m2.get(Constants.version));
        if (res != 0) {
            return res;
        }
        // version are identical, comparing releases:
        return compareStrings(m1.get(Constants.release), m2.get(Constants.release));
    }

    private int compareStrings(String o1, String o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return 1;
        }
        // if we are here - o1 is not null:
        if (o2 == null) {
            return -1;
        }
        // if we are here - none of the arguments is null:
        StringTokenizer tokenizer1 = new StringTokenizer(o1, "-.");
        StringTokenizer tokenizer2 = new StringTokenizer(o2, "-.");
        while (tokenizer1.hasMoreTokens() && tokenizer2.hasMoreTokens()) {
            String t1 = tokenizer1.nextToken();
            String t2 = tokenizer2.nextToken();
            if (allDigits(t1) && allDigits(t2)) {
                int i1 = Integer.parseInt(t1);
                int i2 = Integer.parseInt(t2);
                int intCompared = i1 - i2;
                if (intCompared != 0) {
                    return intCompared > 0 ? -1 : 1;
                }
                continue;
            }
            int stringCompared = t1.compareTo(t2);
            if (stringCompared != 0) {
                return stringCompared > 0 ? -1 : 1;
            }
        }
        // if we are here then one of strings has ended,
        // longer will be considered bigger version:
        if (tokenizer1.hasMoreTokens()) {
            return -1;
        }
        if (tokenizer2.hasMoreTokens()) {
            return 1;
        }
        return 0;
    }

    private String[] composePkgsArray() {
        return composeArray(config.getPackageName());
    }

    private String[] composeArchesArray() {
        return composeArray(config.getArch());
    }

    private static String[] composeArray(String values) {
        if (values == null || values.trim().isEmpty()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(values, ",;\n\r\t ");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    private boolean allDigits(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO maybe implement?
    }

    protected Object execute(String methodName, Object... args) {
        try {
            XmlRpcClient client = createClient();
            Object res = client.execute(methodName, Arrays.asList(args));
            return res;
        } catch (Exception ex) {
            throw new RuntimeException("Exception while executing " + methodName, ex);
        }
    }

    private XmlRpcClient createClient() throws Exception {
        XmlRpcClientConfigImpl xmlRpcConfig = new XmlRpcClientConfigImpl();
        xmlRpcConfig.setServerURL(new URL(config.getKojiTopUrl()));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(xmlRpcConfig);
        client.setTypeFactory(new KojiTypeFactory(client));
        return client;
    }

    private static class KojiTypeFactory extends TypeFactoryImpl {

        public KojiTypeFactory(XmlRpcController pController) {
            super(pController);
        }

        @Override
        public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String
                pLocalName) {
            switch (pLocalName) {
                case "nil":
                    return new NilParser();
                default:
                    return super.getParser(pConfig, pContext, pURI, pLocalName);
            }
        }

        @Override
        public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
            if (pObject instanceof Integer) {
                return new IntSerializer();
            }
            return super.getSerializer(pConfig, pObject);
        }

    }

    private static class NilParser extends AtomicParser {

        @Override
        public void setResult(String pResult) throws SAXException {
            if (pResult != null && pResult.trim().length() > 0) {
                throw new SAXParseException("Unexpected characters in nil element.", getDocumentLocator());
            }
            super.setResult((Object) null);
        }

    }

    private static class IntSerializer extends TypeSerializerImpl {

        @Override
        public void write(ContentHandler pHandler, Object pObject) throws SAXException {
            write(pHandler, I4Serializer.INT_TAG, pObject.toString());
        }

    }

}
