package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public interface TableFormatter {


    String MASTER = AccessibleSettings.master.baseUrl;
    String JENKINS_PORT = "8080"; //FIXME made accessible

    String cellStart(int span);

    String cellStart();

    String cellEnd();

    String rowStart();

    String rowEnd();

    String tableStart();

    String tableEnd();

    String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn);

    String initialCell(String[] project);

    String lastCell(int total, int i);


    public static class PlainTextTableFormatter implements TableFormatter {

        public String cellStart(int span) {
            return "";
        }

        @Override
        public String initialCell(String[] project) {
            if (project == null || project.length == 0) {
                return "all projects";
            }
            if (project.length == 1) {
                return project[0];
            } else {
                return project.length + " projects";
            }
        }

        @Override
        public String lastCell(int found, int all) {
            return found + "/" + all;
        }

        @Override
        public String cellStart() {
            return "";
        }

        @Override
        public String cellEnd() {
            return "";
        }

        @Override
        public String rowStart() {
            return "";
        }

        @Override
        public String rowEnd() {
            return "";
        }

        @Override
        public String tableStart() {
            return "";
        }

        @Override
        public String tableEnd() {
            return "";
        }

        @Override
        public String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn) {
            return "" + l.size();
        }


    }

    public static class HtmlTableFormatter implements TableFormatter {

        private static class DummyLeaf extends MatrixGenerator.Leaf {

            public DummyLeaf(String s) {
                super(s);
            }
        }

        public String cellStart(int span) {
            return cellStart();
        }

        @Override
        public String cellStart() {
            return "<td>";
        }

        @Override
        public String cellEnd() {
            return "</td>";
        }

        @Override
        public String rowStart() {
            return "<tr>";
        }

        @Override
        public String rowEnd() {
            return "</tr>";
        }

        @Override
        public String tableStart() {
            return "<table>";
        }

        @Override
        public String tableEnd() {
            return "</table>";
        }

        @Override
        public String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn) {
            return getContextImpl(l, false, maxInColumn, null);
        }

        protected String getContextImpl(List<MatrixGenerator.Leaf> origL, boolean td, int maxInColumn, String alsoReportVr) {
            List<MatrixGenerator.Leaf> l = new ArrayList<>(maxInColumn);
            l.addAll(origL);
            if (l.isEmpty() && !td) {
                return "0";
            }
            if (td) {
                while (l.size() < maxInColumn) {
                    if (l.size() == 0) {
                        l.add(new DummyLeaf("0"));
                    } else {
                        l.add(new DummyLeaf(""));
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l.size(); i++) {
                MatrixGenerator.Leaf leaf = l.get(i);
                String url = leaf.toString();
                String tdopen = "";
                String tdclose = "";
                if (i == 0) {
                    if (td) {
                        tdclose = "</td>";
                    }
                } else if (i == l.size() - 1) {
                    if (td) {
                        tdopen = "<td>";
                    }
                } else {
                    if (td) {
                        tdopen = "<td>";
                        tdclose = "</td>";
                    }
                }
                if (leaf instanceof DummyLeaf) {
                    sb.append(tdopen + leaf.toString() + tdclose);
                } else
                    try {
                        new URL(url);
                        sb.append(tdopen + openAdd(url) + "<a href=\"" + url + "\">").append("[" + (i + 1) + "]").append("</a>" + closeAdd() + tdclose);
                    } catch (MalformedURLException ex) {
                        String reportHref = "";
                        if (alsoReportVr != null) {
                            reportHref = "<a class=\"reportJump\" href=\"#" + url + "/" + alsoReportVr + "\">[^]</a>";
                        }
                        sb.append(tdopen + openAdd(url) + "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/job/").append(url.toString()).append("\">").append("[" + (i + 1) + "]").append("</a>" + reportHref + closeAdd() + tdclose);
                    }
            }
            return sb.toString();
        }

        protected String openAdd(String job) {
            return "";
        }

        protected String closeAdd() {
            return "";
        }

        @Override
        public String initialCell(String[] project) {
            if (project == null || project.length == 0) {
                return "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/\">all projects</a>";
            }
            if (project.length == 1) {
                return "<a href=\"" + MASTER + ":" + JENKINS_PORT + "/view/~" + project[0] + "#projectstatus\">" + project[0] + "</a>";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < project.length; i++) {
                    String leaf = project[i];
                    sb.append("<a href=\"" + MASTER + ":" + JENKINS_PORT + "/view/~").append(leaf).append("#projectstatus\">").append("[" + (i + 1) + "]").append("</a>");
                }
                return sb.toString();
            }
        }

        @Override
        public String lastCell(int found, int all) {
            return found + "/" + all;
        }
    }

    public static class SpanningHtmlTableFormatter extends HtmlTableFormatter {

        @Override
        public String cellStart(int span) {
            return "<td colspan=\"" + span + "\">";
        }

        @Override
        public String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn) {
            return getContextImpl(l, true, maxInColumn, null);
        }
    }

    public static class SpanningFillingHtmlTableFormatter extends SpanningHtmlTableFormatter {

        private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

        private final String vr;
        private final String projectsRegex;
        private final String jvm;
        private final File remoteJar;
        private static final String remoteJarName = "summary-report-1.0-SNAPSHOT-jar-with-dependencies.jar";
        private final String url;
        private final String dir;
        private final String time;
        private final boolean alsoReport;
        private String cachedReport = "Error to cache report";
        private Map<String, Integer> cachedResults = new HashMap<>(0);

        public SpanningFillingHtmlTableFormatter(String nvr, AccessibleSettings settings, String time, boolean appendReport, String... projects) {
            this.vr = nvr;
            if (projects == null || projects.length == 0) {
                this.projectsRegex = ".*";
            } else {
                this.projectsRegex = ".*-" + String.join("-.*|.*-", projects) + "-.*";
            }
            this.jvm = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            File cpBase = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
            if (!cpBase.isDirectory()) {
                cpBase = cpBase.getParentFile();
            }
            System.err.println(cpBase);
            if (System.getProperty("overwriteCpBaseOfSummaryReport") != null) {
                remoteJar = new File(System.getProperty("overwriteCpBaseOfSummaryReport"), remoteJarName);
            } else {
                remoteJar = new File(cpBase, remoteJarName);
            }
            url = AccessibleSettings.master.baseUrl + ":" + settings.getJenkinsPort();
            dir = settings.getJenkinsJobsRoot().getAbsolutePath();
            if (time == null) {
                this.time = "1";
            } else {
                this.time = time;
            }
            this.alsoReport = appendReport;

        }

        @Override
        public String tableStart() {
            String result = super.tableStart();
            if (alsoReport) {
                File f = null;
                try {
                    f = File.createTempFile("summaryReport", ".cache");
                    ProcessBuilder pb = new ProcessBuilder(jvm, "-jar", remoteJar.getAbsolutePath(),
                            "--directory", dir, "--jenkins", this.url, "--time", time, "--nvrfilter", vr, "--jobfilter", projectsRegex, "--return", "DONE-" + f.getAbsolutePath());
                    cachedReport = executeAndWaitForOutput(pb);
                    cachedResults = new HashMap<>();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
                        while (true) {
                            String l = br.readLine();
                            if (l == null) {
                                break;
                            }
                            String[] keyValue = l.split("\\s+");
                            cachedResults.put(keyValue[0], Integer.valueOf(keyValue[1]));
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to cache results", ex);
                } finally {
                    if (f != null) {
                        f.delete();
                    }
                }

            }
            return result;
        }

        @Override
        public String getContext(List<MatrixGenerator.Leaf> l, int maxInColumn) {
            if (alsoReport) {
                return getContextImpl(l, true, maxInColumn, vr);
            } else {
                return getContextImpl(l, true, maxInColumn, null);
            }
        }

        @Override
        protected String openAdd(String job) {
            int i = 5; //it returns 0green, 1white, 2yellow, 3red, 3< error (4 known 125 unknown..hopefuly)
            if (alsoReport) {
                //use cached results
                Integer ii = cachedResults.get(job);
                if (ii != null) {
                    i = ii;
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder(jvm, "-jar", remoteJar.getAbsolutePath(),
                        "--directory", dir, "--jenkins", this.url, "--time", time, "--nvrfilter", vr, "--jobfilter", job, "--return", "DONE");
                LOGGER.log(Level.INFO, pb.command().toString());
                if (System.getProperty("debugSummaryProcess") != null) {
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                }
                try {
                    Process p = pb.start();
                    i = p.waitFor();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
            }
            String color;
            switch (i) {
                case (0):
                    color = "green";
                    break;
                case (1):
                    color = "lightgreen";
                    break;
                case (2):
                    color = "yellow";
                    break;
                case (3):
                    color = "red";
                    break;
                default:
                    color = "grey";
                    break;
            }
            return "<span style=\"background-color: " + color + "; width:100%; display:block; border:solid ; border-width: 1px\">";
        }

        @Override
        protected String closeAdd() {
            return "</span>";
        }

        @Override
        public String tableEnd() {
            String s = super.tableEnd();
            if (alsoReport) {
                return s + "<hr/>" + cachedReport;
            }
            return s;
        }

        private String executeAndWaitForOutput(ProcessBuilder pb) {
            LOGGER.log(Level.INFO, pb.command().toString());
            if (System.getProperty("debugSummaryProcess") != null) {
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            }
            String result = "Failed to generate report - " + pb.command();
            try {
                final Process p = pb.start();
                StreamGobbler out = new StreamGobbler(p.getInputStream());
                Thread outT = new Thread(out);
                outT.start();
                if (System.getProperty("debugSummaryProcess") == null) {
                    StreamLooser err = new StreamLooser(p.getErrorStream());
                    Thread errT = new Thread(err);
                    errT.start();
                }
                while (!out.done) {
                    Thread.sleep(1000);
                }
                p.waitFor();
                result = out.sb.toString();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
            if (result.trim().isEmpty()) {
                result = "Some error to generate report - " + pb.command();
            }
            return result;
        }

        class StreamLooser implements Runnable {
            private InputStream inputStream;

            public StreamLooser(InputStream inputStream) {
                this.inputStream = inputStream;
            }

            @Override
            public void run() {
                try {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                        while (true) {
                            String l = br.readLine();
                            if (l == null) {
                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
            }
        }

        class StreamGobbler implements Runnable {
            private InputStream inputStream;
            private final StringBuilder sb = new StringBuilder();
            private boolean done = false;

            public StreamGobbler(InputStream inputStream) {
                this.inputStream = inputStream;
            }

            @Override
            public void run() {
                try {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                        while (true) {
                            String l = br.readLine();
                            if (l == null) {
                                break;
                            } else {
                                sb.append(l).append("\n");
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                } finally {
                    done = true;
                }
            }
        }
    }
}
