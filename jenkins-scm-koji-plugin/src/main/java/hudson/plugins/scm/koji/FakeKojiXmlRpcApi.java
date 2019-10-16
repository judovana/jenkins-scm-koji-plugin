package hudson.plugins.scm.koji;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import java.util.Objects;

public class FakeKojiXmlRpcApi extends KojiXmlRpcApi {

    private static final long serialVersionUID = 1869109064394368024L;

    private final String projectName;
    private final String buildVariants;
    private final String buildPlatform;
    private final boolean isBuilt;

    @DataBoundConstructor
    public FakeKojiXmlRpcApi(
            String projectName,
            String buildVariants,
            String buildPlatform,
            boolean isBuilt
    ) {
        super(KojiXmlRpcApiType.FAKE_KOJI);
        this.projectName = projectName;
        this.buildVariants = buildVariants;
        this.buildPlatform = buildPlatform;
        this.isBuilt = isBuilt;
    }

    @Extension
    public static class FakeKojiXmlRpcApiDescriptor extends KojiXmlRpcApiDescriptor {

        public FakeKojiXmlRpcApiDescriptor() {
            super(FakeKojiXmlRpcApi.class, "Fake Koji");
        }
    }

    @Exported
    public String getProjectName() {
        return projectName;
    }

    @Exported
    public String getBuildVariants() {
        return buildVariants;
    }

    @Exported
    public String getBuildPlatform() {
        return buildPlatform;
    }

    @Exported
    public boolean isBuilt() {
        return isBuilt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FakeKojiXmlRpcApi)) return false;
        FakeKojiXmlRpcApi that = (FakeKojiXmlRpcApi) o;
        return isBuilt == that.isBuilt &&
                Objects.equals(projectName, that.projectName) &&
                Objects.equals(buildVariants, that.buildVariants) &&
                Objects.equals(buildPlatform, that.buildPlatform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, buildVariants, buildPlatform, isBuilt);
    }
}
