package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SummaryReportRunner {
    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    private static final String jarName = "summary-report-1.0-SNAPSHOT-jar-with-dependencies.jar";

    private final String nvr;
    private final String projectRegex;
    private final String jvm;
    private final File remoteJar;
    private final String url;
    private final String dir;
    private final String time;
    private final String chartDir;
    private final Optional<String> explicitUrl;

    public SummaryReportRunner(
            final AccessibleSettings settings,
            final String nvr,
            final String time,
            final String chartDir,
            final Optional<String> explicitUrl,
            final String[] projects) {
        this.nvr = nvr;
        this.time = time;
        this.chartDir = chartDir;
        this.explicitUrl = explicitUrl;
        url = settings.getJenkinsUrl();
        dir = settings.getJenkinsJobsRoot().getAbsolutePath();
        projectRegex = projects.length == 0
                ? ".*"
                : ".*-" + String.join("-.*|.*-", projects) + "-.*";
        jvm = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        remoteJar = Optional.ofNullable(System.getProperty("overwriteCpBaseOfSummaryReport"))
                .map(prop -> new File(prop, jarName))
                .orElseGet(() -> {
                    final File cpBase = new File(this.getClass()
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .getPath());
                    return new File(cpBase.isDirectory() ? cpBase : cpBase.getParentFile(), jarName);
                });
    }

    public static class SummaryreportResults {
        public final String report;
        public final Map<String, Integer> nvrResult;
        public final File logFile;

        public SummaryreportResults(String report, Map<String, Integer> nvrResult, File logFile) {
            this.report = report;
            this.nvrResult = nvrResult;
            this.logFile = logFile;
        }
    }


    public Result<SummaryreportResults, String> getSummaryReport() {
        final File tmp;
        try {
            tmp = File.createTempFile("summaryReport", ".cache");
        } catch (IOException e) {
            return Result.err("Failed to create tmp file");
        }
        List<String> args = chartDir != null
                ? getArgs(projectRegex, "DONE-" + tmp.getAbsolutePath())
                : getArgs(projectRegex, "DONE-" + tmp.getAbsolutePath(), chartDir);
        ProcessBuilder pb = new ProcessBuilder(args);
        final Tuple<String,File> cachedReport = executeAndWaitForOutput(pb);
        final HashMap<String, Integer> cachedResults = new HashMap<>();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tmp)))) {
            while (true) {
                String l = br.readLine();
                if (l == null) {
                    break;
                }
                String[] keyValue = l.split("\\s+");
                cachedResults.put(keyValue[0], Integer.valueOf(keyValue[1]));
            }
            return Result.ok(new SummaryreportResults(cachedReport.x, cachedResults, cachedReport.y));
        } catch (IOException e) {
            return Result.err(e.getMessage());
        }
    }

    private List<String> getArgs(final String jobFilter, final String returnPath) {
        List<String> defaults = Arrays.asList(
                jvm,
                "-jar",
                remoteJar.getAbsolutePath(),
                "--directory",
                dir,
                "--jenkins",
                url,
                "--time",
                time,
                "--nvrfilter",
                nvr,
                "--jobfilter",
                jobFilter,
                "--return",
                returnPath,
                "--surpass",
                "best"
        );
        if (explicitUrl.isPresent()) {
            List a = new ArrayList<>(defaults);
            a.add("--explicitcomparsion-url");
            a.add(explicitUrl.get());
            return a;
        } else {
            return defaults;
        }
    }

    private List<String> getArgs(final String jobFilter, final String returnPath, final String chartDir) {
        return Stream.concat(getArgs(jobFilter, returnPath).stream(), Stream.of(
                "--chartdir",
                chartDir,
                "--wipecharts",
                "true",
                "--interpolate",
                "true"
        )).collect(Collectors.toList());
    }

    public int getJobReportSummary(final String jobName) {
        final List<String> args = getArgs(jobName, "DONE");
        final ProcessBuilder pb = new ProcessBuilder(args);
        LOGGER.log(Level.INFO, pb.command().toString());
        if (System.getProperty("debugSummaryProcess") != null) {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        try {
            final Process p = pb.start();
            return p.waitFor();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return 5;
        }
    }

    private Tuple<String, File> executeAndWaitForOutput(ProcessBuilder pb) {
        LOGGER.log(Level.INFO, pb.command().toString());
        String result = "Failed to generate report - " + pb.command();
        File f = new File("not_created");
        try {
            f = File.createTempFile("SummaryReportRunner","err");
            LOGGER.log(Level.INFO, "Errors in: "+f.getAbsolutePath());
            final Process p = pb.start();
            final StreamToFileGobbler err = new StreamToFileGobbler(p.getErrorStream(), f);
            Thread outE = new Thread(err);
            outE.start();
            final StreamGobbler out = new StreamGobbler(p.getInputStream());
            Thread outT = new Thread(out);
            outT.start();
            while (!out.done && !err.done) {
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
        return new Tuple(result, f);
    }

    static class StreamLooser implements Runnable {
        private final InputStream inputStream;

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

    static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
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

    static class StreamToFileGobbler implements Runnable {
        private final InputStream inputStream;
        private final File file;
        private boolean done = false;

        public StreamToFileGobbler(InputStream inputStream, File f) {
            this.inputStream = inputStream;
            this.file = f;
        }

        @Override
        public void run() {
            try {
                try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
                    ;
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                        while (true) {
                            String l = br.readLine();
                            if (l == null) {
                                break;
                            } else {
                                bw.write(l);
                                bw.newLine();
                            }
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
