package hudson.plugins.scm.koji.model;

public class KojiScmConfig implements java.io.Serializable {

    private final String packageName;
    private final String arch;
    private final String tag;
    private final String excludeNvr;
    private final String whitelistNvr;
    private final String downloadDir;
    private final boolean cleanDownloadDir;
    private final boolean dirPerNvr;
    private final int maxPreviousBuilds;

    //testing only
    public KojiScmConfig(
                         String packageName,
                         String arch,
                         String tag,
                         String excludeNvr,
                         String downloadDir,
                         boolean cleanDownloadDir,
                         boolean dirPerNvr,
                         int maxPreviousBuilds) {
        this(packageName, arch, tag, excludeNvr, null, downloadDir, cleanDownloadDir, dirPerNvr, maxPreviousBuilds);
    }
    public KojiScmConfig(
                         String packageName,
                         String arch,
                         String tag,
                         String excludeNvr,
                         String whitelistNvr,
                         String downloadDir,
                         boolean cleanDownloadDir,
                         boolean dirPerNvr,
                         int maxPreviousBuilds) {
        this.packageName = packageName;
        this.arch = arch;
        this.tag = tag;
        this.excludeNvr = excludeNvr;
        this.whitelistNvr = whitelistNvr == null || whitelistNvr.isEmpty() ? "*" : whitelistNvr;
        this.downloadDir = downloadDir;
        this.cleanDownloadDir = cleanDownloadDir;
        this.dirPerNvr = dirPerNvr;
        this.maxPreviousBuilds = maxPreviousBuilds;
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

    public String getWhitelistNvr() {
        return whitelistNvr;
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

    @Override
    public String toString() {
        return "KojiScmConfig{"
                + "packageName='" + packageName + '\''
                + ", arch='" + arch + '\''
                + ", tag='" + tag + '\''
                + ", excludeNvr='" + excludeNvr + '\''
                + ", downloadDir='" + downloadDir + '\''
                + ", cleanDownloadDir=" + cleanDownloadDir
                + ", dirPerNvr=" + dirPerNvr
                + ", maxPreviousBuilds=" + maxPreviousBuilds
                + '}';
    }
}
