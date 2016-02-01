package hudson.plugins.scm.koji;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static hudson.plugins.scm.koji.Constants.KOJI_CHECKOUT_NVR;
import static hudson.plugins.scm.koji.Constants.PROCESSED_BUILDS_HISTORY;

public class KojiSCM extends SCM {

    @Extension
    public static final KojiScmDescriptor DESCRIPTOR = new KojiScmDescriptor();
    private static final Logger LOG = LoggerFactory.getLogger(KojiSCM.class);
    private String kojiTopUrl;
    private String kojiDownloadUrl;
    private String packageName;
    private String arch;
    private String tag;
    private String excludeNvr;
    private String downloadDir;
    private boolean cleanDownloadDir;
    private boolean dirPerNvr;

    @DataBoundConstructor
    public KojiSCM(String kojiTopUrl, String kojiDownloadUrl, String packageName, String arch, String tag, String excludeNvr, String downloadDir, boolean cleanDownloadDir, boolean dirPerNvr) {
        this.kojiTopUrl = kojiTopUrl;
        this.kojiDownloadUrl = kojiDownloadUrl;
        this.packageName = packageName;
        this.arch = arch;
        this.tag = tag;
        this.excludeNvr = excludeNvr;
        this.downloadDir = downloadDir;
        this.cleanDownloadDir = cleanDownloadDir;
        this.dirPerNvr = dirPerNvr;
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
        LOG.info("Checking out remote revision");
        if (baseline != null && !(baseline instanceof KojiRevisionState)) {
            throw new RuntimeException("Expected instance of KojiRevisionState, got: " + baseline);
        }

        // TODO add some flag to allow checkout on local or remote machine
        KojiBuildDownloader downloadWorker = new KojiBuildDownloader(listener.getLogger()::println, createConfig(),
                createNotProcessedNvrPredicate(run.getParent()));
        Optional<KojiBuildDownloadResult> buildOpt = workspace.act(downloadWorker);

        if (!buildOpt.isPresent()) {
            LOG.info("Checkout finished without any results");
            listener.getLogger().println("No updates.");
            return;
        }

        KojiBuildDownloadResult downloadResult = buildOpt.get();
        Build build = downloadResult.getBuild();
        LOG.info("Checkout downloaded build: {}", build);

        LOG.info("Saving the nvr of checked out build to history: {} >> {}", build.getNvr(), PROCESSED_BUILDS_HISTORY);
        Files.write(
                new File(run.getParent().getRootDir(), PROCESSED_BUILDS_HISTORY).toPath(),
                Arrays.asList(build.getNvr()),
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        LOG.info("Saving the nvr of checked out build to workspace: {} > {}", build.getNvr(), KOJI_CHECKOUT_NVR);
        Files.write(new File(run.getParent().getRootDir(), KOJI_CHECKOUT_NVR).toPath(),
                Arrays.asList(build.getNvr()),
                Charset.forName("UTF-8"),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        // if there is a changelog file - write it:
        if (changelogFile != null) {
            LOG.info("Saving the build info to changelog file: {}", changelogFile.getAbsolutePath());
            new BuildsSerializer().write(build, changelogFile);
        }

        run.addAction(new KojiEnvVarsAction(build.getNvr(), downloadResult.getRpmsDirectory(),
                downloadResult.getRpmFiles().stream().sequential().collect(Collectors.joining(File.pathSeparator))));

    }

    @Override
    public PollingResult compareRemoteRevisionWith(Job<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
        LOG.info("Comparing remote revision with: {}", baseline);
        if (!(baseline instanceof KojiRevisionState)) {
            throw new RuntimeException("Expected instance of KojiRevisionState, got: " + baseline);
        }

        KojiListBuilds worker = new KojiListBuilds(listener.getLogger()::println, createConfig(),
                createNotProcessedNvrPredicate(project));
        Optional<Build> buildOpt = workspace.act(worker);

        if (buildOpt.isPresent()) {
            Build build = buildOpt.get();
            LOG.info("Got new remote build: {}", build);
            return new PollingResult(baseline, new KojiRevisionState(build), PollingResult.Change.INCOMPARABLE);
        }
        // if we are still here - no remote changes:
        LOG.info("No remote changes");
        return new PollingResult(baseline, null, PollingResult.Change.NONE);
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        LOG.info("Calculating revision for project '{}' from build: {}", run.getParent().getName(), run.getNumber());
        KojiRevisionFromBuild worker = new KojiRevisionFromBuild();
        FilePath buildWorkspace = new FilePath(run.getRootDir());
        Optional<Build> buildOpt = buildWorkspace.act(worker);
        if (buildOpt.isPresent()) {
            Build build = buildOpt.get();
            LOG.info("Got revision from build {}: {}", run.getNumber(), build.getNvr());
            return new KojiRevisionState(build);
        }
        LOG.info("No build info found");
        return new KojiRevisionState(null);
    }

    @Override
    public boolean supportsPolling() {
        return true;
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return true;
    }

    private Predicate<String> createNotProcessedNvrPredicate(Job<?, ?> job) throws IOException {
        File processedNvrFile = new File(job.getRootDir(), PROCESSED_BUILDS_HISTORY);
        if (processedNvrFile.exists()) {
            if (processedNvrFile.isFile() && processedNvrFile.canRead()) {
                Set<String> nvrsSet = Files
                        .lines(processedNvrFile.toPath(), StandardCharsets.UTF_8)
                        .collect(Collectors.toSet());
                return new NotProcessedNvrPredicate(nvrsSet);
            } else {
                throw new IOException("Processed NVRs is not readable: " + processedNvrFile.getAbsolutePath());
            }
        } else {
            return new NotProcessedNvrPredicate(new HashSet<>());
        }
    }

    private KojiScmConfig createConfig() {
        return new KojiScmConfig(kojiTopUrl, kojiDownloadUrl, packageName, arch, tag, excludeNvr, downloadDir,
                cleanDownloadDir, dirPerNvr);
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

}
