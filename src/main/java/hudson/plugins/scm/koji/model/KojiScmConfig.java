package hudson.plugins.scm.koji.model;

public class KojiScmConfig {

    private final String kojiTopUrl;
    private final String kojiDownloadUrl;
    private final String packageName;
    private final String arch;
    private final String tag;
    private final String excludeNvr;

    public KojiScmConfig(String kojiTopUrl, String kojiDownloadUrl, String packageName, String arch, String tag, String excludeNvr) {
        this.kojiTopUrl = kojiTopUrl;
        this.kojiDownloadUrl = kojiDownloadUrl;
        this.packageName = packageName;
        this.arch = arch;
        this.tag = tag;
        this.excludeNvr = excludeNvr;
    }

    public String getKojiTopUrl() {
        return kojiTopUrl;
    }

    public String getKojiDownloadUrl() {
        return kojiDownloadUrl;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getArch() {
        return arch;
    }

    public String getTag() {
        return tag;
    }

    public String getExcludeNvr() {
        return excludeNvr;
    }

}
