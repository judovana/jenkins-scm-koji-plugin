package org.fakekoji.core.utils.matrix;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public SummaryReportRunner(
            final AccessibleSettings settings,
            final String nvr,
            final String time,
            final String chartDir,
            final String[] projects
    ) {
        this.nvr = nvr;
        this.time = time;
        this.chartDir = chartDir;
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

    public Result<Tuple<String, Map<String, Integer>>, String> getSummaryReport() {
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
        final String cachedReport = executeAndWaitForOutput(pb);
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
            return Result.ok(new Tuple<>(cachedReport, cachedResults));
        } catch (IOException e) {
            return Result.err(e.getMessage());
        }
    }

    private List<String> getArgs(final String jobFilter, final String returnPath) {
        return Arrays.asList(
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
                returnPath
        );
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

    private String executeAndWaitForOutput(ProcessBuilder pb) {
        LOGGER.log(Level.INFO, pb.command().toString());
        if (System.getProperty("debugSummaryProcess") != null) {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        String result = "Failed to generate report - " + pb.command();
        try {
            final Process p = pb.start();
            final StreamGobbler out = new StreamGobbler(p.getInputStream());
            Thread outT = new Thread(out);
            outT.start();
            if (System.getProperty("debugSummaryProcess") == null) {
                final StreamLooser err = new StreamLooser(p.getErrorStream());
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
}
