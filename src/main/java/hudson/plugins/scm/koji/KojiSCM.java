package hudson.plugins.scm.koji;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.plugins.scm.koji.client.KojiBuildDownloader;
import hudson.plugins.scm.koji.client.KojiListBuilds;
import hudson.plugins.scm.koji.model.KojiBuildDownloadResult;
import hudson.plugins.scm.koji.model.KojiScmConfig;
import hudson.plugins.scm.koji.model.Build;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static hudson.plugins.scm.koji.Constants.BUILD_ENV_NVR;
import static hudson.plugins.scm.koji.Constants.PROCESSED_BUILDS_HISTORY;
import static hudson.plugins.scm.koji.Constants.KOJI_CHECKOUT_NVR;

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

    @DataBoundConstructor
    public KojiSCM(String kojiTopUrl, String kojiDownloadUrl, String packageName, String arch, String tag, String excludeNvr) {
        this.kojiTopUrl = kojiTopUrl;
        this.kojiDownloadUrl = kojiDownloadUrl;
        this.packageName = packageName;
        this.arch = arch;
        this.tag = tag;
        this.excludeNvr = excludeNvr;
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

        KojiBuildDownloader downloadWorker = new KojiBuildDownloader(listener.getLogger()::println, createConfig(), new NotProcessedNvrPredicate(workspace));
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
        workspace.act(new KojiProcessedAppender(build));

        LOG.info("Saving the nvr of checked out build to workspace: {} > {}", build.getNvr(), KOJI_CHECKOUT_NVR);
        workspace.act(new KojiCheckoutNvr(build));

        // if there is a changelog file - write it:
        if (changelogFile != null) {
            LOG.info("Saving the build info to changelog file: {}", changelogFile.getAbsolutePath());
            new BuildsSerializer().write(build, changelogFile);
        }

        LOG.info("Adding env variable to the build: '{}={}'", BUILD_ENV_NVR, build.getNvr());
        run.addAction(new ParametersAction(new StringParameterValue(BUILD_ENV_NVR, build.getNvr())));
    }

    @Override
    public PollingResult compareRemoteRevisionWith(Job<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
        LOG.info("Comparing remote revision with: {}", baseline);
        if (!(baseline instanceof KojiRevisionState)) {
            throw new RuntimeException("Expected instance of KojiRevisionState, got: " + baseline);
        }

        KojiListBuilds worker = new KojiListBuilds(listener.getLogger()::println, createConfig(), new NotProcessedNvrPredicate(workspace));
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
        return false;
    }

    private KojiScmConfig createConfig() {
        return new KojiScmConfig(kojiTopUrl, kojiDownloadUrl, packageName, arch, tag, excludeNvr);
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

}
