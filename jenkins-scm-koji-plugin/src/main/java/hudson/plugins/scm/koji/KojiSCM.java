package hudson.plugins.scm.koji;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.scm.koji.client.KojiBuildDownloader;
import hudson.plugins.scm.koji.client.KojiListBuilds;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.KojiBuildDownloadResult;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.function.Predicate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static hudson.plugins.scm.koji.Constants.BUILD_XML;
import static hudson.plugins.scm.koji.Constants.PROCESSED_BUILDS_HISTORY;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class KojiSCM extends SCM implements LoggerHelp, Serializable {

    @Extension
    public static final KojiScmDescriptor DESCRIPTOR = new KojiScmDescriptor();
    private static final Logger LOG = LoggerFactory.getLogger(KojiSCM.class);
    private static final boolean verbose = true;
    private String kojiTopUrl;
    private String kojiDownloadUrl;
    private String packageName;
    private String arch;
    private String tag;
    private String excludeNvr;
    private String downloadDir;
    private boolean cleanDownloadDir;
    private boolean dirPerNvr;
    private int maxPreviousBuilds;
    private transient TaskListener currentListener;

    private boolean canLog() {
        return (verbose && currentListener != null && currentListener.getLogger() != null);
    }

    private String host() {
        try {
            String h = InetAddress.getLocalHost().getHostName();
            if (h == null) {
                return "null";
            } else {
                return h;
            }
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    void print(String s) {
        try {
            currentListener.getLogger().println(s);
        } catch (Exception ex) {
            LOG.error("During printing of log to TaskListener", ex);
        }
    }

    @Override
    public void log(String s) {
        LOG.info(s);
        if (canLog()) {
            print("[KojiSCM][" + host() + "] " + s);
        }
    }

    @Override
    public void log(String s, Object o) {
        LOG.info(s, o);
        if (canLog()) {
            if (o == null) {
                o = "null";
            }
            print("[KojiSCM][" + host() + "] " + s + ": " + o.toString());
        }
    }

    @Override
    public void log(String s, Object... o) {
        LOG.info(s, o);
        if (canLog()) {
            print("[KojiSCM][" + host() + "] " + s);
            for (Object object : o) {
                if (object == null) {
                    object = "null";
                }
                print("[KojiSCM]   " + object.toString());
            }
        }
    }

    @DataBoundConstructor
    public KojiSCM(String kojiTopUrl, String kojiDownloadUrl, String packageName, String arch, String tag, String excludeNvr, String downloadDir, boolean cleanDownloadDir, boolean dirPerNvr, int maxPreviousBuilds) {
        this.kojiTopUrl = kojiTopUrl;
        this.kojiDownloadUrl = kojiDownloadUrl;
        this.packageName = packageName;
        this.arch = arch;
        this.tag = tag;
        this.excludeNvr = excludeNvr;
        this.downloadDir = downloadDir;
        this.cleanDownloadDir = cleanDownloadDir;
        this.dirPerNvr = dirPerNvr;
        this.maxPreviousBuilds = maxPreviousBuilds;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new KojiChangeLogParser();
    }

    @Override
    public SCMDescriptor<?> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void checkout(Run<?, ?> run, Launcher launcher, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
        currentListener = listener;
        log("Checking out remote revision");
        if (baseline != null && !(baseline instanceof KojiRevisionState)) {
            throw new RuntimeException("Expected instance of KojiRevisionState, got: " + baseline);
        }

        File checkoutBuildFile = new File(run.getParent().getRootDir(), BUILD_XML);
        KojiBuildDownloader downloadWorker = new KojiBuildDownloader(createConfig(),
                createNotProcessedNvrPredicate(run.getParent()),
                new BuildsSerializer().read(checkoutBuildFile));
        downloadWorker.setListener(listener);
        KojiBuildDownloadResult downloadResult = workspace.act(downloadWorker);

        if (downloadResult == null) {
            log("Checkout finished without any results");
            listener.getLogger().println("No updates.");
            throw new AbortException("Checkout was invoked but no remote changes found");
        }

        final Build build = downloadResult.getBuild();
        log("Checkout downloaded build: {}", build);

        String displayName = build.getVersion() + "-" + build.getRelease();
        log("Updating the build name to: {}", displayName);
        if (build.isManual()) {
            run.setDisplayName(displayName + "(manual)");
        } else {
            run.setDisplayName(displayName);
        }
        if (build.isManual()) {
            log("manual mode -  not saving the nvr of checked out build to history: {} >> {}", build.getNvr(), PROCESSED_BUILDS_HISTORY);
        } else {
            log("Saving the nvr of checked out build to history: {} >> {}", build.getNvr(), PROCESSED_BUILDS_HISTORY);
            appendBuildNvrToProcessed(new File(run.getParent().getRootDir(), PROCESSED_BUILDS_HISTORY), build);
        }
        // if there is a changelog file - write it:
        if (changelogFile != null) {
            log("Saving the build info to changelog file: {}", changelogFile.getAbsolutePath());
            new BuildsSerializer().write(build, changelogFile);
        }

        run.addAction(new KojiEnvVarsAction(build.getNvr(), downloadResult.getRpmsDirectory(),
                String.join(File.pathSeparator, downloadResult.getRpmFiles())));
    }

    private static final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_XXX");

    static void appendBuildNvrToProcessed(File processed, final Build build) throws IOException {
        appendStringProcessed(processed, build.getNvr() + "  # " + formatter.format(new Date()));
    }

    static void appendStringProcessed(File processed, final String nvr) throws IOException {
        Files.write(
                processed.toPath(),
                Collections.singletonList(nvr),
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    @Override
    public PollingResult compareRemoteRevisionWith(Job<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
        currentListener = listener;
        log("Comparing remote revision with: {}", baseline);
        if (!(baseline instanceof KojiRevisionState)) {
            throw new RuntimeException("Expected instance of KojiRevisionState, got: " + baseline);
        }

        KojiListBuilds worker = new KojiListBuilds(createConfig(), createNotProcessedNvrPredicate(project));
        Build build;
        if (!DESCRIPTOR.getKojiSCMConfig()) {
            // when requiresWorkspaceForPolling is set to false (based on descriptor), worksapce may be null.
            // but not always.  So If it os not null, the path to it is passed on.
            // however, its usage may be invalid. See KojiListBuilds.invole comemnt about BUILD_XML
            File wFile = null;
            if (workspace != null) {
                wFile = new File(workspace.toURI().getPath());
            }
            build = worker.invoke(wFile, null);
        } else {
            build = workspace.act(worker);
        }

        if (build != null) {
            log("Got new remote build: {}", build);
            log("Saving {} to {}", build, BUILD_XML);
            new BuildsSerializer().write(build, new File(project.getRootDir(), BUILD_XML));
            return new PollingResult(baseline, new KojiRevisionState(build), PollingResult.Change.INCOMPARABLE);
        }
        // if we are still here - no remote changes:
        log("No remote changes");
        return new PollingResult(baseline, null, PollingResult.Change.NONE);
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        currentListener = listener;
        log("Calculating revision for project '{}' from build: {}", run.getParent().getName(), run.getNumber());
        KojiRevisionFromBuild worker = new KojiRevisionFromBuild();
        FilePath buildWorkspace = new FilePath(run.getRootDir());
        Build build = buildWorkspace.act(worker);
        if (build != null) {
            log("Got revision from build {}: {}", run.getNumber(), build.getNvr());
            return new KojiRevisionState(build);
        }
        log("No build info found");
        return new KojiRevisionState(null);
    }

    @Override
    public boolean supportsPolling() {
        return true;
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        // this is merchandize - if it is true, then the jobs can not run in parallel (se "Execute concurrent builds if necessary" in project settings)
        // when it is false, projects can run inparalel, but pooling operation do not have workspace
        return DESCRIPTOR.getKojiSCMConfig();
    }

    private Predicate<String> createNotProcessedNvrPredicate(Job<?, ?> job) throws IOException {
        File processedNvrFile = new File(job.getRootDir(), PROCESSED_BUILDS_HISTORY);
        return NotProcessedNvrPredicate.createNotProcessedNvrPredicateFromFile(processedNvrFile);
    }

    private KojiScmConfig createConfig() {
        return new KojiScmConfig(kojiTopUrl, kojiDownloadUrl, packageName, arch, tag, excludeNvr, downloadDir,
                cleanDownloadDir, dirPerNvr, maxPreviousBuilds);
    }

    public String getKojiTopUrl() {
        return kojiTopUrl;
    }

    @DataBoundSetter
    public void setKojiTopUrl(String kojiTopUrl) {
        this.kojiTopUrl = kojiTopUrl;
    }

    public String getKojiDownloadUrl() {
        return kojiDownloadUrl;
    }

    @DataBoundSetter
    public void setKojiDownloadUrl(String kojiDownloadUrl) {
        this.kojiDownloadUrl = kojiDownloadUrl;
    }

    public String getPackageName() {
        return packageName;
    }

    @DataBoundSetter
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getArch() {
        return arch;
    }

    @DataBoundSetter
    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getTag() {
        return tag;
    }

    @DataBoundSetter
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getExcludeNvr() {
        return excludeNvr;
    }

    @DataBoundSetter
    public void setExcludeNvr(String excludeNvr) {
        this.excludeNvr = excludeNvr;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    @DataBoundSetter
    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public boolean isCleanDownloadDir() {
        return cleanDownloadDir;
    }

    @DataBoundSetter
    public void setCleanDownloadDir(boolean cleanDownloadDir) {
        this.cleanDownloadDir = cleanDownloadDir;
    }

    public boolean isDirPerNvr() {
        return dirPerNvr;
    }

    @DataBoundSetter
    public void setDirPerNvr(boolean dirPerNvr) {
        this.dirPerNvr = dirPerNvr;
    }

    public int getMaxPreviousBuilds() {
        return maxPreviousBuilds;
    }

    @DataBoundSetter
    public void setMaxPreviousBuilds(int maxPreviousBuilds) {
        this.maxPreviousBuilds = maxPreviousBuilds;
    }

}
