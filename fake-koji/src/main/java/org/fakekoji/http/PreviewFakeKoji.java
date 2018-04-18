/*
 * The MIT License
 *
 * Copyright 2017.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fakekoji.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import hudson.plugins.scm.koji.NotProcessedNvrPredicate;
import hudson.plugins.scm.koji.client.BuildMatcher;
import hudson.plugins.scm.koji.client.GlobPredicate;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fakekoji.JavaServer;
import org.fakekoji.xmlrpc.server.core.FakeBuild;

/**
 * This class implements http server, which allows to review builds in
 * fake-koji, in human readable form
 */
public class PreviewFakeKoji {

    private final IndexHtmlHandler ihh;
    private final DetailsHtmlHandler dhh;
    private final SettingsHtmlHandler ghh;
    private HttpServer hs;

    private final String jenkinsUrlOverride;
    private final AccessibleSettings settings;

    public PreviewFakeKoji(AccessibleSettings settings) throws MalformedURLException {
        this.settings = settings;
        this.ihh = new IndexHtmlHandler(settings.getXmlRpcUrl(), settings.getDownloadUrl(), Project.FilesToProjects(settings.getLocalReposRoot()), Product.FilesToBuilds(settings.getDbFileRoot()), getPort());
        this.dhh = new DetailsHtmlHandler(settings.getXmlRpcUrl(), settings.getDownloadUrl(), Project.FilesToProjects(settings.getLocalReposRoot()), Product.FilesToBuilds(settings.getDbFileRoot()), getPort());
        this.ghh = new SettingsHtmlHandler(settings);
        jenkinsUrlOverride = settings.getJenkinsUrlString().toExternalForm();
    }

    public void start() throws IOException {
        if (hs == null) {
            hs = HttpServer.create(new InetSocketAddress(settings.getPreview1Port()), 0);
            hs.createContext("/", ihh);
            hs.createContext("/details.html", dhh);
            hs.createContext("/get", ghh);
        }
        hs.start();
    }

    public final int getPort() {
        return settings.getPreview1Port();
    }

    public void stop() {
        if (hs != null) {
            hs.stop(15);
        }
    }

    public static URL setUriPort(URL uri, int port) throws MalformedURLException {
        URL newUri = new URL(uri.getProtocol(), uri.getHost(), port, uri.getFile());
        return newUri;
    }

    public static void main(String[] args) throws MalformedURLException, IOException {
        //defualting to 80 with all consequences
/*          //debugging
         args = new String[]{
         "http://hydra.brq.redhat.com/RPC2/",
         "http://hydra.brq.redhat.com/",
         "/mnt/raid1/upstream-repos",
         "/mnt/raid1/local-builds",
         //"1919"
         };*/
        if (args.length != 5) {
            System.out.println("Mandatory 5 params:  xmlport, downloadpoport, reposPath, bnuildsPath, httpPort");
        }
        int xmlrpcurl = Integer.valueOf(args[0]);
        int download = Integer.valueOf(args[1]);
        File repos = new File(args[2]);
        File builds = new File(args[3]);
        if (!builds.exists()) {
            throw new RuntimeException(builds.getAbsolutePath() + " does not exists.");
        }
        if (!repos.exists()) {
            throw new RuntimeException(repos.getAbsolutePath() + " does not exists.");
        }

        int PORT = Integer.valueOf(args[4]);

        System.err.println("xmlrpc   : " + xmlrpcurl);
        System.out.println("xmlrpc   : " + xmlrpcurl);
        System.err.println("dwnld    : " + download);
        System.out.println("dwnld    : " + download);
        System.err.println("port     : " + PORT);
        System.out.println("port     : " + PORT);
        System.err.println("repos    : " + repos);
        System.out.println("repos    : " + repos);
        System.err.println("builds   : " + builds);
        System.out.println("builds   : " + builds);

        AccessibleSettings settings = new AccessibleSettings(builds, repos, xmlrpcurl, download, JavaServer.DFAULT_SSHUPLOAD_PORT, PORT, JavaServer.DEFAULT_JENKINS_PORT);

        PreviewFakeKoji previewFakeKojiServer = new PreviewFakeKoji(settings);
        previewFakeKojiServer.start();
    }

    private static List<File> filter(File[] candidates) {
        List<File> r = new ArrayList<>(candidates.length);
        for (File candidate : candidates) {
            if (candidate.isDirectory()) {
                Path pf = candidate.toPath();
                if (!Files.isSymbolicLink(pf)) {
                    r.add(candidate);
                }
            }
        }
        Collections.sort(r);
        Collections.reverse(r);
        return r;
    }

    private static class Project extends Product {

        public Project(File backedn) {
            super(backedn);
        }

        public static List<Project> FilesToProjects(File dir) {
            List<File> candidates = filter(dir.listFiles());
            List<Project> r = new ArrayList<>(candidates.size());
            for (File f : candidates) {
                r.add(new Project(f));
            }
            return r;
        }

        public String getSuffixToString() {
            String s = getSuffix();
            if (s.isEmpty()) {
                return getName();
            }
            return s;
        }

        public String getSuffix() {
            if (getName().matches("^java-[0-9.]*-openjdk.*")) {
                String[] s = getName().split("-");
                if (s.length < 3) {
                    throw new RuntimeException("Strange repo " + getName());
                }
                if (s.length == 3) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < s.length; i++) {
                    sb.append("-").append(s[i]);
                }
                return sb.substring(1);
            } else {
                return "";
            }
        }

        public String getPrefix() {
            if (getName().matches("^java-[0-9.]*-openjdk.*")) {
                String[] s = getName().split("-");
                if (s.length < 3) {
                    throw new RuntimeException("Strange repo " + getName());
                }
                if (s.length == 3) {
                    return getName();
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    sb.append("-").append(s[i]);
                }
                return sb.substring(1);
            } else {
                return getName();
            }
        }

    }

    private static class Product {

        private final File backend;

        public Product(File backend) {
            this.backend = backend;
        }

        public static List<Product> FilesToBuilds(File dir) {
            List<File> candidates = filter(dir.listFiles());
            List<Product> r = new ArrayList<>(candidates.size());
            for (File f : candidates) {
                r.add(new Product(f));
            }
            return r;
        }

        public File getDir() {
            return backend.getAbsoluteFile();
        }

        public String getName() {
            return backend.getName();
        }

        /*
         VERY infrastrucutre specific
         */
        public String getJenkinsMapping() {
            if (getName().equals("java-1.8.0-openjdk")) {
                return "ojdk8";
            }
            if (getName().equals("java-1.7.0-openjdk")) {
                return "ojdk7";
            }
            if (getName().equals("java-9-openjdk")) {
                return "ojdk9";
            }
            return getName();
        }

    }

    public class IndexHtmlHandler implements HttpHandler {

        private final URL xmlrpc;
        private final URL dwnld;
        private final List<Project> projects;
        private final List<Product> products;
        private final int port;

        private IndexHtmlHandler(URL xmlrpcurl, URL download, List<Project> projects, List<Product> builds, int port) {
            xmlrpc = xmlrpcurl;
            dwnld = download;
            this.projects = projects;
            this.products = builds;
            this.port = port;

        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            RequestRunner rr = new RequestRunner(exchange);
            new Thread(rr).start();
        }

        private String getJenkinsUrl() {
            if (jenkinsUrlOverride != null) {
                return jenkinsUrlOverride;
            }
            return getUrl(8080);
        }

        private String getUrl() {
            return getUrl(port);
        }

        private String getUrl(int port) {
            return dwnld.getProtocol() + "://" + dwnld.getHost() + ":" + port;
        }

        private class RequestRunner implements Runnable {

            private final HttpExchange t;

            public RequestRunner(HttpExchange t) {
                this.t = t;
            }

            @Override
            public void run() {
                try {
                    runImpl();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            public void runImpl() throws IOException {
                String requestedFile = t.getRequestURI().getPath();
                System.out.println(new Date().toString() + " attempting: " + requestedFile);
                generateIndex(t);
            }
        }

        private void generateIndex(HttpExchange t) throws IOException {
            System.out.println("Regenerating index!");
            StringBuilder sb = generateIndex();
            String result = sb.toString();
            long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
            t.sendResponseHeaders(200, size);
            try (OutputStream os = t.getResponseBody()) {
                os.write(result.getBytes());
            }
        }

        private StringBuilder generateIndex() throws IOException {
            final StringBuilder sb = new StringBuilder();
            sb.append("<html>\n");
            sb.append("  <body>\n");
            sb.append("  <h1>").append("Latest builds of upstream OpenJDK").append("</h1>\n");
            for (Product product : products) {
                sb.append("  <a href='#").append(product.getName()).append("'>").append(product.getName()).append("</a>\n");
            }
            sb.append("  <p>")
                    .append("if you are searching for failed builds or logs or test results, feel free to search directly in <a href='")
                    .append(getJenkinsUrl())
                    .append("'>jenkins</a> in connection with direct listing in <a href='")
                    .append(dwnld.toExternalForm())
                    .append("'>fake-koji</a>").append("</p>\n");
            sb.append("  <p>")
                    .append("Total ").append(products.size())
                    .append(" products from ").append(projects.size()).append(" projects.")
                    .append("</p>\n");
            for (Product product : products) {
                sb.append("<blockquote>");
                List<Build> buildObjects = getBuildsfForProduct(product, xmlrpc);
                sb.append("  <a name='").append(product.getName()).append("'/>\n");
                sb.append("  <h2>").append(product.getName()).append("</h2>\n");
                int fastdebugs = 0;
                for (Object buildObject : buildObjects) {
                    if (buildObject.toString().contains("fastdebug")) {
                        fastdebugs++;
                    }
                    System.err.println(buildObject);
                }
                //now this is nasty, and very infrastructure specific
                //we have to  filter projects valid to producets. Thats doen by prefix
                List<Project> validProjects = new ArrayList<>(projects.size());
                for (Project project : projects) {
                    if (project.getPrefix().equals(product.getName())) {
                        //should be already sorted by string
                        validProjects.add(project);
                    }
                }
                for (Project validProject : validProjects) {
                    sb.append("  <a href='#PR-").append(validProject.getName()).append("'>").append(validProject.getSuffixToString()).append("</a>\n");
                }
                sb.append("  <p>")
                        .append("Found ").append(buildObjects.size())
                        .append(" successful builds and ").append(validProjects.size()).append(" relevant projects")
                        .append(" From those ").append(Integer.valueOf(buildObjects.size() - fastdebugs)).append(" are normal builds.")
                        .append(" and ").append(fastdebugs).append(" are fastdebug builds.")
                        .append("</p>\n");
                sb.append("  <p>")
                        .append("You can see all  ").append(buildObjects.size())
                        .append(" builds details <a href='details.html?list=")
                        .append(joinListOfBuilds(buildObjects))
                        .append("'> here</a>.")
                        .append(" You can see jenkins build results in <a href='").append(getJenkinsUrl()).append("/search/?q=build-static-").append(product.getJenkinsMapping()).append("'> here</a>.")
                        .append(" You can see jenkins TEST results in <a href='").append(getJenkinsUrl()).append("/search/?max=2000&q=").append(product.getJenkinsMapping()).append("'> here</a>.")
                        .append("</p>\n");
                List<Build> usedBuilds = new ArrayList<>(buildObjects.size());
                //now wee need to filetr only project's products
                // the suffix is projected to *release*
                //and is behind leading NUMBER. and have all "-" repalced by "."
                //in addition we need to strip all keywords. usually usptream or usptream.fastdebug or static
                //yes, oh crap
                for (Project validProject : validProjects) {
                    List<Build> projectsBuilds = new ArrayList<>(buildObjects.size());
                    Map<String, Build> projectsFastdebugBuilds = new HashMap<>(buildObjects.size());
                    sb.append("  <a name='PR-").append(validProject.getName()).append("'/>");
                    sb.append("  <h3>").append(validProject.getSuffixToString()).append("</h3>\n");

                    for (Object object : buildObjects) {
                        //also we need to remove all fastdebug products and ony offer them together with normal of same VRA
                        Build built = (Build) object;
                        String cleanedSuffix = built.getRelease();
                        cleanedSuffix = cleanedSuffix.replaceAll(".upstream.*", "");
                        cleanedSuffix = cleanedSuffix.replaceAll(".static.*", "");
                        String suffixCandidate = "";
                        if (cleanedSuffix.contains(".")) {
                            suffixCandidate = cleanedSuffix.substring(cleanedSuffix.indexOf(".") + 1);
                        }
                        String relaseLikeSuffix = validProject.getSuffix().replaceAll("-", ".");
                        if (suffixCandidate.equals(relaseLikeSuffix)) {
                            usedBuilds.add(built);
                            if (built.getRelease().contains("fastdebug")) {
                                projectsFastdebugBuilds.put(built.getNvr().replaceAll(".fastdebug", ""), built);
                            } else {
                                projectsBuilds.add(built);
                            }
                        }
                    }
                    Build build = null;
                    if (!projectsBuilds.isEmpty()) {
                        build = projectsBuilds.get(projectsBuilds.size() - 1);
                    }
                    offerBuild(build, projectsFastdebugBuilds, sb, projectsBuilds, dwnld);

                }
                if (usedBuilds.size() != buildObjects.size()) {
                    List<Build> unUsedBuilds = new ArrayList<>(-usedBuilds.size() + buildObjects.size());
                    for (Object bo : buildObjects) {
                        if (usedBuilds.contains((Build) bo)) {

                        } else {
                            unUsedBuilds.add((Build) bo);
                        }
                    }
                    sb.append("  <h4>There are unsorted ").append(unUsedBuilds.size()).append(" builds: </h4>\n");
                    sb.append("  <p>").append("You can see them ").append(detailsLink("here", unUsedBuilds)).append(".</p>\n");
                }
                sb.append("</blockquote>");
            }
            sb.append("  </body>\n");
            sb.append("</html>\n");
            return sb;
        }

    }

    public static class DetailsHtmlHandler implements HttpHandler {

        private final URL xmlrpc;
        private final URL dwnld;
        private final List<Project> projects;
        private final List<Product> products;
        private final int port;

        private DetailsHtmlHandler(URL xmlrpcurl, URL download, List<Project> projects, List<Product> builds, int port) {
            xmlrpc = xmlrpcurl;
            dwnld = download;
            this.projects = projects;
            this.products = builds;
            this.port = port;

        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            RequestRunner rr = new RequestRunner(exchange);
            new Thread(rr).start();

        }

        private class RequestRunner implements Runnable {

            private final HttpExchange t;

            public RequestRunner(HttpExchange t) {
                this.t = t;
            }

            @Override
            public void run() {
                try {
                    runImpl();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            public void runImpl() throws IOException {
                String requestedFile = t.getRequestURI().getPath();
                System.out.println(new Date().toString() + " attempting: " + requestedFile);
                generateIndex(t);
            }
        }

        private void generateIndex(HttpExchange t) throws IOException {
            System.out.println("Regenerating details!");
            Map<String, String> query = splitQuery(t.getRequestURI());
            StringBuilder sb = new StringBuilder("No list specified!");
            if (query.get("list") != null && !query.get("list").isEmpty()) {
                sb = generateDetailsFor(query.get("list"));
            }
            String result = sb.toString();
            long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
            t.sendResponseHeaders(200, size);
            try (OutputStream os = t.getResponseBody()) {
                os.write(result.getBytes());
            }
        }

        private StringBuilder generateDetailsFor(String get) {
            StringBuilder sb = new StringBuilder();
            String[] list = get.split(",");
            sb.append("<html>\n");
            sb.append("  <body>\n");
            sb.append("  <h1>").append("Details for: </h1>\n");
            sb.append("  <p1>");
            for (String build : list) {
                sb.append(" | <a href='#").append(build).append("'> ").append(build).append(" </a>");
            }
            sb.append(" | </p1>\n");
            //get projects
            Set<Product> usedProducets = new HashSet<>(products.size());
            products.stream().forEach((product) -> {
                for (String build : list) {
                    if (build.startsWith(product.getName())) {
                        usedProducets.add(product);
                    }
                }
            });
            usedProducets.stream().forEach((product) -> {
                sb.append("  <h2>").append(product.getName()).append("</h2>\n");
                List<Build> buildObjects = getBuildsfForProduct(product, xmlrpc);
                List<Build> allBuilds = new ArrayList<>(buildObjects.size());
                List<Build> usedBuilds = new ArrayList<>(buildObjects.size());
                Map<String, Build> allProjectsFastdebugBuilds = new HashMap<>(buildObjects.size());
                List<Build> requestedFastDebugBuilds = new ArrayList<>(buildObjects.size());
                List<Build> unusedBuilds = new ArrayList<>(buildObjects.size());
                for (Object buildObject : buildObjects) {
                    Build build = (Build) buildObject;
                    boolean wonted = false;
                    for (String s : list) {
                        if (s.equals(build.getNvr())) {
                            wonted = true;
                            break;
                        }
                    }
                    allBuilds.add(build);
                    if (build.getNvr().contains("fastdebug")) {
                        allProjectsFastdebugBuilds.put(build.getNvr().replaceAll(".fastdebug", ""), build);
                        if (wonted) {
                            requestedFastDebugBuilds.add(build);
                        } else {
                            unusedBuilds.add(build);
                        }
                    } else if (wonted) {
                        usedBuilds.add(build);
                    } else {
                        unusedBuilds.add(build);
                    }

                }
                sb.append("<p> showing ")
                        .append(detailsLink(usedBuilds))
                        .append(" builds + ")
                        .append(detailsLink(requestedFastDebugBuilds))
                        .append(" explicit fastdebug builds from toal of ")
                        .append(detailsLink(allBuilds))
                        .append(" You may wont to see ")
                        .append(detailsLink(unusedBuilds))
                        .append(" missing builds")
                        .append("</p>\n");
                if (!usedBuilds.isEmpty()) {
                    if (BuildMatcher.compareBuildsByCompletionTime(usedBuilds.get(0), usedBuilds.get(usedBuilds.size() - 1)) > 0) {
                        Collections.reverse(usedBuilds);
                    }
                }
                if (!requestedFastDebugBuilds.isEmpty()) {
                    if (BuildMatcher.compareBuildsByCompletionTime(requestedFastDebugBuilds.get(0), requestedFastDebugBuilds.get(requestedFastDebugBuilds.size() - 1)) > 0) {
                        Collections.reverse(requestedFastDebugBuilds);
                    }
                }
                for (Build usedBuild : usedBuilds) {
                    sb.append("<a name='").append(usedBuild.getNvr()).append("'/>");
                    offerBuild(usedBuild, allProjectsFastdebugBuilds, sb, null, dwnld);
                }
                sb.append("  <h2> Explicitly requested fastdebug builds:</h2>\n");
                for (Build usedBuild : requestedFastDebugBuilds) {
                    sb.append("<a name='").append(usedBuild.getNvr()).append("'/>");
                    offerBuild(usedBuild, null, sb, null, dwnld);
                }
            });
            sb.append("  </body>\n");
            sb.append("</html>\n");
            return sb;
        }
    }

    public static Map<String, String> splitQuery(URI url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        if (query == null) {
            return query_pairs;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx >= 0) {
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
        }
        return query_pairs;
    }

    private static void offerBuild(Build build, Map<String, Build> projectsFastdebugBuilds, StringBuilder sb, List<Build> projectsBuilds, URL dwnld) {
        sb.append("<blockquote>");
        if (build == null) {
            sb.append("  <p>").append("Sorry, no successful build matches this project. You may check all builds of this product.").append("</p><br/>\n");
        } else {
            sb.append("  <b>").append(build.getNvr()).append("</b><br/>\n");
            sb.append("   <small>").append(build.getCompletionTime()).append("</small><br/>\n");
            sb.append(offerDownloads(dwnld.toExternalForm(), build));
            if (projectsFastdebugBuilds != null) {
                Build bb = projectsFastdebugBuilds.get(build.getNvr());
                if (bb == null) {
                    sb.append("  <i>no fast debug build found</i><br/>\n");
                } else {
                    sb.append("  <i>").append(bb.getNvr()).append("</i><br/>\n");
                    sb.append("   <i><small>").append(bb.getCompletionTime()).append("</small></i><br/>\n");
                    sb.append("<small>\n");
                    sb.append(offerDownloads(dwnld.toExternalForm(), bb));
                    sb.append("</small><br/>\n");
                }
            }
            if (projectsBuilds != null) {
                List<Build> l = new ArrayList<>(projectsBuilds.size() + projectsFastdebugBuilds.size());
                l.addAll(projectsBuilds);
                l.addAll(projectsFastdebugBuilds.values());
                sb.append("  <p>")
                        .append("You can see all  ").append(projectsBuilds.size())
                        .append(" builds details (and ").append(projectsFastdebugBuilds.size())
                        .append(" fastdebug builds) ").append(detailsLink("here", l))
                        .append(".</p>\n");
            }
        }
        sb.append("</blockquote>");
    }

    public static String offerDownloads(String baseUrl, Build build) {
        List<RPM> rpms = new ArrayList<>(build.getRpms());
        Collections.sort(rpms, new Comparator<RPM>() {
            @Override
            public int compare(RPM o1, RPM o2) {
                return o1.getArch().compareTo(o2.getArch());
            }
        });
        String sourceSnapshot = "";
        StringBuilder r = new StringBuilder();
        r.append("<blockquote>");
        for (RPM rpm : rpms) {
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/";
            }
            String mainUrl = baseUrl + rpm.getName() + "/" + rpm.getVersion() + "/" + rpm.getRelease();
            String archedUrl = mainUrl + "/" + rpm.getArch();
            String logsUrl = mainUrl + "/data/logs/" + rpm.getArch();
            String filename = rpm.getFilename("tarxz");
            String fileUrl = archedUrl + "/" + filename;
            if (rpm.getArch().equals("src")) {
                sourceSnapshot = "sources: <a href='" + fileUrl + "'>src snapshot</a> <a href='" + logsUrl + "'>hg incomming logs</a><br/>";
            } else {
                r.append("<b>").append(rpm.getArch()).append("</b>: \n");
                r.append("<a href='")
                        .append(fileUrl)
                        .append("'>")
                        .append(filename)
                        .append("</a> <a href='")
                        .append(logsUrl)
                        .append("'>build logs</a><br/>");

            }
        }
        r.append(sourceSnapshot);
        r.append("</blockquote>");
        return r.toString();
    }

    private static String joinListOfBuilds(Collection<Build> lb) {
        if (lb.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Build b : lb) {
            sb.append(",").append(b.getNvr());
        }
        return sb.substring(1);
    }

    private static List<Build> getBuildsfForProduct(Product product, URL xmlrpc) {
        Object[] all = getBuildfForProduct(product, xmlrpc);
        List<Build> typedNotRunnign = new ArrayList<>(all.length);
        for (Object object : all) {
            Build o = (Build) object;
            boolean running = false;
            Set<String> tags = o.getTags();
            for (String tag : tags) {
                if (tag.contains(FakeBuild.notBuiltTagPart)) {
                    running = true;
                    break;
                }
            }
            if (!running) {
                typedNotRunnign.add(o);
            }
        }
        return typedNotRunnign;
    }

    private static Object[] getBuildfForProduct(Product product, URL xmlrpc) {
        //get all products of top-project
        Object[] buildObjects = new BuildMatcher(
                xmlrpc.toExternalForm(),
                new NotProcessedNvrPredicate(new ArrayList<>()),
                new GlobPredicate("*"),
                5000, product.getName(), null).getAll();
        return buildObjects;
    }

    private static String detailsLink(Collection<Build> items) {
        return detailsLink("" + items.size(), items);

    }

    private static String detailsLink(String text, Collection<Build> items) {
        return "<a href='details.html?list=" + joinListOfBuilds(items) + "'> " + text + "</a>";
    }

    public static class SettingsHtmlHandler implements HttpHandler {

        private final AccessibleSettings settings;

        public SettingsHtmlHandler(AccessibleSettings settings) {
            this.settings = settings;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            RequestRunner rr = new RequestRunner(exchange);
            new Thread(rr).start();

        }

        private class RequestRunner implements Runnable {

            private final HttpExchange t;

            public RequestRunner(HttpExchange t) {
                this.t = t;
            }

            @Override
            public void run() {
                try {
                    runImpl();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            public void runImpl() throws IOException {
                String requestedFile = t.getRequestURI().getPath();
                String query = t.getRequestURI().getQuery();
                System.out.println(new Date().toString() + " attempting: " + requestedFile + " with " + query);
                returnValue(t);
            }

            private static final String GET_HELP = "use get?property  or get?property1&property2&...propertN or get?help to list values";

            private void returnValue(HttpExchange t) throws IOException {
                String rawQuery = t.getRequestURI().getQuery();
                ResultWithHttpCode result = getResultFor(rawQuery);
                long size = result.getResponse().length(); //yahnot perfect, ets assuemno one will use this on chinese chars
                t.sendResponseHeaders(result.getReturnCode().getvalue(), size);
                try (OutputStream os = t.getResponseBody()) {
                    os.write(result.getResponse() .getBytes());
                }
            }

            private ResultWithHttpCode getResultFor(String rawQuery) {
                final StringBuilder response = new StringBuilder();
                ReturnCode returnCode = ReturnCode.OK;
                if (rawQuery == null) {
                    response.append(GET_HELP);
                } else {
                    String[] query = rawQuery.split("&");
                    if (query.length == 0 || rawQuery.trim().isEmpty()) {
                        response.append(GET_HELP);
                        returnCode = ReturnCode.BAD_REQUEST;
                    } else {
                        if (query.length == 1) {
                            ResultWithHttpCode result = getResult(query[0]);
                            response.append(result.getResponse());
                            returnCode = result.getReturnCode();
                        } else {
                            for (String key : query) {
                                ResultWithHttpCode result = getResult(key);
                                if (returnCode != ReturnCode.BAD_REQUEST) {
                                    returnCode = result.getReturnCode();
                                }
                                response.append(key).append("=");
                                response.append(result.getResponse());
                                response.append("\n");
                            }
                        }
                    }
                }
                return new ResultWithHttpCode(response.toString(), returnCode);
            }

            private ResultWithHttpCode getResult(String parameter) {
                if (null == parameter) {
                    return null;
                }
                String[] paramSplit = parameter.split(":");
                String property = paramSplit[0];
                String value = paramSplit.length == 2 ? paramSplit[1] : null;
                try {
                    return new ResultWithHttpCode(settings.get(property, value), ReturnCode.OK);
                } catch (ProjectMappingExceptions.ProjectMappingException e) {
                    return new ResultWithHttpCode(e.getMessage(), ReturnCode.BAD_REQUEST);
                }
            }

            private class ResultWithHttpCode {
                private String response;
                private ReturnCode returnCode;

                ResultWithHttpCode(String response, ReturnCode returnCode) {
                    this.response = response;
                    this.returnCode = returnCode;
                }

                public void setResponse(String response) {
                    this.response = response;
                }

                public void setReturnCode(ReturnCode returnCode) {
                    this.returnCode = returnCode;
                }

                String getResponse() {
                    return response;
                }

                ReturnCode getReturnCode() {
                    return returnCode;
                }
            }
        }
        private enum ReturnCode {
            OK(200),
            BAD_REQUEST(400);

            private final int code;
            ReturnCode(int code) {
                this.code = code;
            }
            public int getvalue() {
                return code;
            }
        }
    }
}
