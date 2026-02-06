package hudson.plugins.scm.koji;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import java.util.Objects;

public class RealKojiXmlRpcApi extends KojiXmlRpcApi {

    private static final long serialVersionUID = 3357707636679152628L;

    private final String packageName;
    private final String arch;
    private final String tag;
    private final String subpackageBlacklist;
    private final String subpackageWhitelist;

    @DataBoundConstructor
    public RealKojiXmlRpcApi(
            String packageName,
            String arch,
            String tag,
            String subpackageBlacklist,
            String subpackageWhitelist
    ) {
        super(KojiXmlRpcApiType.REAL_KOJI);
        this.packageName = packageName;
        this.arch = arch;
        this.tag = tag;
        this.subpackageBlacklist = subpackageBlacklist;
        this.subpackageWhitelist = subpackageWhitelist;
    }

    @Extension
    public static class RealKojiXmlRpcApiDescriptor extends KojiXmlRpcApiDescriptor {

        public RealKojiXmlRpcApiDescriptor() {
            super(RealKojiXmlRpcApi.class, "Real Koji");
        }
    }

    @Exported
    public String getPackageName() {
        return packageName;
    }

    @Exported
    public String getArch() {
        return arch;
    }

    @Exported
    public String getTag() {
        return tag;
    }

    @Exported
    public String getSubpackageBlacklist() {
        return subpackageBlacklist;
    }

    @Exported
    public String getSubpackageWhitelist() {
        return subpackageWhitelist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RealKojiXmlRpcApi)) return false;
        RealKojiXmlRpcApi that = (RealKojiXmlRpcApi) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(arch, that.arch) &&
                Objects.equals(tag, that.tag) &&
                Objects.equals(subpackageBlacklist, that.subpackageBlacklist) &&
                Objects.equals(subpackageWhitelist, that.subpackageWhitelist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, arch, tag, subpackageBlacklist, subpackageWhitelist);
    }

    @Override
    public String toString() {
        return
                "Koji xml-rpc api:\n" +
                "  packageName: " + packageName + '\n' +
                "  arch: " + arch + '\n' +
                "  tag: " + tag + '\n' +
                "  subpackageBlacklist: " + subpackageBlacklist + '\n' +
                "  subpackageWhitelist: " + subpackageWhitelist + '\n';
    }
}
