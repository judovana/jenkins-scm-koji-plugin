package hudson.plugins.scm.koji.model;

public class KojiScmConfig implements java.io.Serializable {

    private final String kojiTopUrl;
    private final String kojiDownloadUrl;
    private final String packageName;
    private final String arch;
    private final String tag;
    private final String excludeNvr;
    private final String downloadDir;
    private final boolean cleanDownloadDir;
    private final boolean dirPerNvr;
    private final int maxPreviousBuilds;

    public KojiScmConfig(String kojiTopUrl, String kojiDownloadUrl, String packageName, String arch, String tag, String excludeNvr, String downloadDir, boolean cleanDownloadDir, boolean dirPerNvr, int maxPreviousBuilds) {
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

    public String getDownloadDir() {
        return downloadDir;
    }

    public boolean isCleanDownloadDir() {
        return cleanDownloadDir;
    }

    public boolean isDirPerNvr() {
        return dirPerNvr;
    }

    public int getMaxPreviousBuilds() {
        return maxPreviousBuilds;
    }

}
