package hudson.plugins.scm.koji.client;

import hudson.FilePath;
import hudson.plugins.scm.koji.BuildsSerializer;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jenkinsci.remoting.RoleChecker;
import static hudson.plugins.scm.koji.Constants.BUILD_XML;
import hudson.plugins.scm.koji.WebLog;
import java.util.function.Predicate;

public class KojiListBuilds extends AbstractKojiClient implements FilePath.FileCallable<Optional<Build>> {

    private static final DateTimeFormatter DTF = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.MICRO_OF_SECOND)
            .toFormatter();

    private final KojiScmConfig config;
    private final GlobPredicate tagPredicate;
    private final Predicate<String> notProcessedNvrPredicate;

    public KojiListBuilds(WebLog log, KojiScmConfig config, Predicate<String> notProcessedNvrPredicate) {
        super(log, config.getKojiTopUrl());
        this.config = config;
        this.tagPredicate = new GlobPredicate(config.getTag());
        this.notProcessedNvrPredicate = notProcessedNvrPredicate;
    }

    @Override
    public Optional<Build> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Optional<Build> buildOpt = listMatchingBuilds()
                .filter(b -> notProcessedNvrPredicate.test(b.getNvr()))
                .findFirst();
        if (buildOpt.isPresent()) {
            Build build = buildOpt.get();
            new BuildsSerializer().write(build, new File(workspace, BUILD_XML));
        }
        return buildOpt;
    }

    private Stream<Build> listMatchingBuilds() {
        log("Starting listing of builds");
        log("Package name: " + config.getPackageName());
        log("Arch: " + config.getArch());
        log("Tag: " + config.getTag());

        log("Requesting package id...");
        Integer packageId = (Integer) execute("getPackageID", config.getPackageName());
        log("Package ID: " + packageId);

        log("Requesting list of builds...");
        Map paramsMap = new HashMap();
        paramsMap.put("packageID", packageId);
        paramsMap.put("state", 1);
        paramsMap.put("__starstar", Boolean.TRUE);

        Object[] results = (Object[]) execute("listBuilds", paramsMap);
        if (results == null || results.length < 1) {
            log("List of builds is empty, finishing.");
            return Stream.empty();
        }
        log("Builds list size: " + results.length);
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
                .filter(this::filterByArch)
                // do not go too far away into the past:
                .limit(10)
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
        Object buildName = m.get("nvr");
        log("Requesting tags for build: " + buildName);

        Map paramsMap = new HashMap();
        paramsMap.put("build", m.get("build_id"));
        paramsMap.put("__starstar", Boolean.TRUE);

        Object res = execute("listTags", paramsMap);
        if (res != null && (res instanceof Object[]) && ((Object[]) res).length > 0) {
            log("Tags: " + Arrays.stream((Object[]) res)
                    .map(t -> ((Map<String, String>) t).get("name"))
                    .collect(Collectors.joining(", ")));
            m.put("tags", res);
        } else {
            log("Build has no tags");
        }
        return o;
    }

    private boolean filterByTags(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;
        Object buildName = m.get("name");
        Object[] tags = (Object[]) m.get("tags");
        if (tags == null) {
            log(buildName + " has no tags, filtered out");
            return false;
        }
        boolean tagMatch = Arrays
                .stream(tags)
                .map(t -> ((Map<String, String>) t).get("name"))
                .anyMatch(tagPredicate);
        log(buildName + (tagMatch ? " tags match." : " has no matching tags, filtered out"));
        return tagMatch;
    }

    private Object retrieveRPMs(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map m = (Map) o;
        Object buildName = m.get("nvr");
        log("Requesting RPMs for build: " + buildName);

        Map paramsMap = new HashMap();
        paramsMap.put("buildID", m.get("build_id"));
        String arch = config.getArch();
        if (arch != null && arch.length() > 0) {
            paramsMap.put("arches", config.getArch());
        }
        paramsMap.put("__starstar", Boolean.TRUE);

        Object res = execute("listRPMs", paramsMap);
        if (res != null && (res instanceof Object[]) && ((Object[]) res).length > 0) {
            log("RPMs: " + Arrays.stream((Object[]) res)
                    .map(r -> ((Map<String, String>) r).get("nvr"))
                    .collect(Collectors.joining(", ")));
            m.put("rpms", res);
        } else {
            log("Build has no RPMs");
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
        boolean hasRpms = m.get("rpms") != null;
        if (!hasRpms) {
            log("Build has no RPMs, filtered out");
        }
        return hasRpms;
    }

    private Build toBuild(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }

        Map<String, String> m = (Map) o;
        return new Build((Integer) ((Map) o).get("build_id"),
                m.get("name"),
                m.get("version"),
                m.get("release"),
                m.get("nvr"),
                dateKojiToIso(m.get("completion_time")),
                Arrays
                .stream((Object[]) ((Map) o).get("rpms"))
                .map(this::toRPM)
                .collect(Collectors.toList()),
                Arrays
                .stream((Object[]) ((Map) o).get("tags"))
                .map(t -> ((Map<String, String>) t).get("name"))
                .collect(Collectors.toSet())
        );
    }

    private RPM toRPM(Object o) {
        if (!(o instanceof Map)) {
            throw new RuntimeException("Map instance expected, got: " + o);
        }
        Map<String, String> m = (Map) o;
        return new RPM(
                m.get("name"),
                m.get("version"),
                m.get("release"),
                m.get("nvr"),
                m.get("arch"));
    }

    private String dateKojiToIso(String kojiDate) {
        LocalDateTime date = LocalDateTime.parse(kojiDate, DTF);
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
        int res = compareStrings(m1.get("version"), m2.get("version"));
        if (res != 0) {
            return res;
        }
        // version are identical, comparing releases:
        return compareStrings(m1.get("release"), m2.get("release"));
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

}
