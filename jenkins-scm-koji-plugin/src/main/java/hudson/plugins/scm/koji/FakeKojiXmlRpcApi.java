package hudson.plugins.scm.koji;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import java.util.Objects;

public class FakeKojiXmlRpcApi extends KojiXmlRpcApi {

    private static final long serialVersionUID = 1869109064394368024L;

    private final String projectName;
    private final String jvm;
    private final String buildVariant;
    private final String buildPlatform;
    private final boolean isBuilt;

    @DataBoundConstructor
    public FakeKojiXmlRpcApi(
            String projectName,
            String jvm,
            String buildVariant,
            String buildPlatform,
            boolean isBuilt
    ) {
        super(KojiXmlRpcApiType.FAKE_KOJI);
        this.projectName = projectName;
        this.jvm = jvm;
        this.buildVariant = buildVariant;
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
    public String getJvm() {
        return jvm;
    }

    @Exported
    public String getBuildVariant() {
        return buildVariant;
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
                Objects.equals(jvm, that.jvm) &&
                Objects.equals(buildVariant, that.buildVariant) &&
                Objects.equals(buildPlatform, that.buildPlatform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, jvm, buildVariant, buildPlatform, isBuilt);
    }
}