package hudson.plugins.scm.koji.model;

import org.fakekoji.xmlrpc.server.JavaServerConstants;

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

    private String getKojiTopUrlInterpreted() {
        return replaceXPORT(getKojiTopUrl());
    }

    public static String replaceXPORT(String source) {
        return testableUrlReplacer(":"+JavaServerConstants.xPortAxiom, ":" + JavaServerConstants.DFAULT_RP2C_PORT, source);
    }

    public String[] getKojiTopUrls() {
        return testableSplitter(getKojiTopUrlInterpreted());
    }

    public String[] getKojiDownloadUrls() {
        return testableSplitter(getKojiDownloadUrlInterpreted());
    }

    public String getKojiDownloadUrl() {
        return kojiDownloadUrl;
    }

    private String getKojiDownloadUrlInterpreted() {
        return replaceDPORT(kojiDownloadUrl);
    }

    public static String replaceDPORT(String source) {
        return testableUrlReplacer(":"+JavaServerConstants.dPortAxiom, ":" + JavaServerConstants.DFAULT_DWNLD_PORT, source);
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

    @Override
    public String toString() {
        return "KojiScmConfig{"
                + "kojiTopUrl='" + kojiTopUrl + '\''
                + ", kojiDownloadUrl='" + kojiDownloadUrl + '\''
                + ", packageName='" + packageName + '\''
                + ", arch='" + arch + '\''
                + ", tag='" + tag + '\''
                + ", excludeNvr='" + excludeNvr + '\''
                + ", downloadDir='" + downloadDir + '\''
                + ", cleanDownloadDir=" + cleanDownloadDir
                + ", dirPerNvr=" + dirPerNvr
                + ", maxPreviousBuilds=" + maxPreviousBuilds
                + '}';
    }

    public static String testableUrlReplacer(String wildchar, String replacement, String source) {
        return source.replaceAll(wildchar, replacement);
    }

    public static String[] testableSplitter(String source) {
        return source.split("\\s+");
    }
}
