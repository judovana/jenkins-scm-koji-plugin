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
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.fakekoji.xmlrpc.server.JavaServer;

public class PreviewFakeKoji {

    public static URL setUriPort(URL uri, int port) throws MalformedURLException {
        URL newUri = new URL(uri.getProtocol(), uri.getHost(), port, uri.getFile());
        return newUri;
    }

    public static void main(String[] args) throws MalformedURLException, IOException {
        //defualting to 80 with all consequences
        args = new String[]{
            "http://hydra.brq.redhat.com/RPC2/",
            "http://hydra.brq.redhat.com/",
            "/mnt/raid1/upstream-repos",
            "/mnt/raid1/local-builds", 
            "1919"
        };
        if (args.length < 3) {
            System.out.println("Mandatory 4 params:  full koji xmlrpc url and koji download url and cloned forests homes and projects homes");
            System.out.println("if no port is specified, my favorit XPORT and DPORT are used,");
            System.out.println("If only first port is specified, second is deducted from it as for fake-koji");
            System.out.println("last param is optional and is port of this service. If missing, 80 is used!");
        }
        URL xmlrpcurl = new URL(args[0]);
        URL download = new URL(args[1]);
        File repos = new File(args[2]);
        File builds = new File(args[3]);
        if (!builds.exists()) {
            throw new RuntimeException(builds.getAbsolutePath() + " does not exists.");
        }
        if (!repos.exists()) {
            throw new RuntimeException(repos.getAbsolutePath() + " does not exists.");
        }

        if (xmlrpcurl.getPort() < 0) {
            xmlrpcurl = setUriPort(xmlrpcurl, JavaServer.DFAULT_RP2C_PORT);
        }
        if (download.getPort() < 0) {
            download = setUriPort(download, JavaServer.deductDwPort(xmlrpcurl.getPort()));
        }

        int PORT = 80;
        if (args.length == 5) {
            PORT = Integer.valueOf(args[4]);
        }
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
        new FakeKojiPreviewServer(xmlrpcurl, download, PORT, repos, builds).start();

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
        }

        public String getPrefix() {
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

    private static class FakeKojiPreviewServer {

        private final int port;
        private final IndexHtmlHandler ihh;
        private final DetailsHtmlHandler dhh;

        public FakeKojiPreviewServer(URL xmlrpcurl, URL download, int PORT, File repos, File builds) {
            port = PORT;
            ihh = new IndexHtmlHandler(xmlrpcurl, download, Project.FilesToProjects(repos), Product.FilesToBuilds(builds), port);
            dhh = new DetailsHtmlHandler(xmlrpcurl, download, Project.FilesToProjects(repos), Product.FilesToBuilds(builds), port);
        }

        private void start() throws IOException {
            HttpServer hs = HttpServer.create(new InetSocketAddress(port), 0);
            hs.createContext("/", ihh);
            hs.createContext("/details.html", dhh);
            hs.start();
        }
    }

    public static class IndexHtmlHandler implements HttpHandler {

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
                System.out.println(new Date().toString() + "attempting: " + requestedFile);
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
                //get all products of top-project
                Object[] buildObjects = new BuildMatcher(
                        xmlrpc.toExternalForm(),
                        new NotProcessedNvrPredicate(new HashSet<>()),
                        new GlobPredicate("*"),
                        5000, product.getName(), null).getAll();
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
                        .append("Found ").append(buildObjects.length)
                        .append(" successful builds and ").append(validProjects.size()).append(" relevant projects")
                        .append(" From those ").append(Integer.valueOf(buildObjects.length - fastdebugs)).append(" are normal builds.")
                        .append(" and ").append(fastdebugs).append(" are fastdebug builds.")
                        .append("</p>\n");
                sb.append("  <p>")
                        .append("You can see all  ").append(buildObjects.length)
                        .append(" builds details <a href='details.html?list=TODO1'> here</a>.")
                        .append(" You can jenkins build results in <a href='").append(getJenkinsUrl()).append("/search/?q=build-static-").append(product.getJenkinsMapping()).append("'> here</a>.")
                        .append(" You can jenkins TEST results in <a href='").append(getJenkinsUrl()).append("/search/?max=2000&q=").append(product.getJenkinsMapping()).append("'> here</a>.")
                        .append("</p>\n");
                List<Build> usedBuilds = new ArrayList<>(buildObjects.length);
                //now wee need to filetr only project's products
                // the suffix is projected to *release*
                //and is behind leading NUMBER. and have all "-" repalced by "."
                //in addition we need to strip all keywords. usually usptream or usptream.fastdebug or static
                //yes, oh crap
                for (Project validProject : validProjects) {
                    List<Build> projectsBuilds = new ArrayList<>(buildObjects.length);
                    Map<String, Build> projectsFastdebugBuilds = new HashMap<>(buildObjects.length);
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
                if (usedBuilds.size() != buildObjects.length) {
                    List<Build> unUsedBuilds = new ArrayList<>(-usedBuilds.size() + buildObjects.length);
                    for (Object bo : buildObjects) {
                        if (usedBuilds.contains((Build) bo)) {

                        } else {
                            unUsedBuilds.add((Build) bo);
                        }
                    }
                    sb.append("  <h3>There are unsorted ").append(unUsedBuilds.size()).append(" builds: </h3>\n");
                    sb.append("  <p>")
                            .append("You can see them <a href='details.html?list=TODO3'> here</a>.")
                            .append("</p>\n");
                }
                sb.append("</blockquote>");
            }
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
                System.out.println(new Date().toString() + "attempting: " + requestedFile);
                generateIndex(t);
            }
        }

        private void generateIndex(HttpExchange t) throws IOException {
            System.out.println("Regenerating index!");
            StringBuilder sb = new StringBuilder("todo: show detaisl of all builds given as &list= parameter");
            String result = sb.toString();
            long size = result.length(); //yahnot perfect, ets assuemno one will use this on chinese chars
            t.sendResponseHeaders(200, size);
            try (OutputStream os = t.getResponseBody()) {
                os.write(result.getBytes());
            }
        }
    }

    private static void offerBuild(Build build, Map<String, Build> projectsFastdebugBuilds, StringBuilder sb, List<Build> projectsBuilds, URL dwnld) {
        sb.append("<blockquote>");
        if (build == null) {
            sb.append("  <p>").append("Sorry, no successful build matches this project. You may check all builds of this product.").append("</p><br/>\n");
        } else {
            sb.append("  <b>").append(build.getNvr()).append("</b><br/>\n");
            sb.append("   <small>").append(build.getCompletionTime()).append("</small><br/>\n");
            sb.append(offerDownloads(dwnld.toExternalForm(), build));
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
            if (projectsBuilds != null) {
                sb.append("  <p>")
                        .append("You can see all  ").append(projectsBuilds.size())
                        .append(" builds details (and ").append(projectsFastdebugBuilds.size()).append(" fast builds) <a href='details.html?list=TODO2'> here</a>.")
                        .append("</p>\n");
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

}
