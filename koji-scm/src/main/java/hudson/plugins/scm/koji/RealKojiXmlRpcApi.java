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
    private final String subpackageDenylist;
    private final String subpackageAllowlist;

    @DataBoundConstructor
    public RealKojiXmlRpcApi(
            String packageName,
            String arch,
            String tag,
            String subpackageDenylist,
            String subpackageAllowlist
    ) {
        super(KojiXmlRpcApiType.REAL_KOJI);
        this.packageName = packageName;
        this.arch = arch;
        this.tag = tag;
        this.subpackageDenylist = subpackageDenylist;
        this.subpackageAllowlist = subpackageAllowlist;
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
    public String getSubpackageDenylist() {
        return subpackageDenylist;
    }

    @Exported
    public String getSubpackageAllowlist() {
        return subpackageAllowlist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RealKojiXmlRpcApi)) return false;
        RealKojiXmlRpcApi that = (RealKojiXmlRpcApi) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(arch, that.arch) &&
                Objects.equals(tag, that.tag) &&
                Objects.equals(subpackageDenylist, that.subpackageDenylist) &&
                Objects.equals(subpackageAllowlist, that.subpackageAllowlist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, arch, tag, subpackageDenylist, subpackageAllowlist);
    }

    @Override
    public String toString() {
        return
                "Koji xml-rpc api:\n" +
                "  packageName: " + packageName + '\n' +
                "  arch: " + arch + '\n' +
                "  tag: " + tag + '\n' +
                "  subpackageDenylist: " + subpackageDenylist + '\n' +
                "  subpackageAllowlist: " + subpackageAllowlist + '\n';
    }
}
