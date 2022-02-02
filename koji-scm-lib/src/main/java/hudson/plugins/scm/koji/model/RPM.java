package hudson.plugins.scm.koji.model;

import hudson.plugins.scm.koji.Constants;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.Arrays;
import java.util.Optional;

@XmlAccessorType(XmlAccessType.FIELD)
public class RPM implements java.io.Serializable {

    @XmlElement(name = Constants.name)
    private final String name;
    @XmlElement(name = Constants.version)
    private final String version;
    @XmlElement(name = Constants.release)
    private final String release;
    @XmlElement(name = Constants.nvr)
    private final String nvr;
    @XmlElement(name = Constants.arch)
    private final String arch;
    @XmlElement(name = Constants.filename)
    private final String filename;
    private String url;
    private String hashSum;

    public RPM(String name, String version, String release, String nvr, String arch, String filename) {
        this.name = name;
        this.version = version;
        this.release = release;
        if (filename == null || filename.endsWith(".rpm") || filename.endsWith(".tarxz")) {
            this.nvr = nvr;
        } else {
            this.nvr = filename;
        }
        this.arch = arch;
        this.filename = filename;
    }

    public RPM(
            String name,
            String version,
            String release,
            String nvr,
            String arch,
            String filename,
            String url
    ) {
        this(name, version, release, nvr, arch, filename);
        this.url = url;
    }

    public RPM() {
        this.name = null;
        this.version = null;
        this.release = null;
        this.nvr = null;
        this.arch = null;
        this.filename = null;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getRelease() {
        return release;
    }

    public String getNvr() {
        return nvr;
    }

    public String getArch() {
        return arch;
    }

    @Override
    public String toString() {
        if (Suffix.INSTANCE.endsWithSuffix(nvr)) {
            return nvr;
        }
        Suffix suffix = Suffix.INSTANCE;
        return nvr + '.' + arch + '.' + suffix.getSuffix(url, filename);
    }

    public String getFilename(String suffix) {
        return Optional.ofNullable(filename).orElse(nvr + '.' + arch + "." + suffix);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean hasUrl() {
        return url != null;
    }

    public void setHashSum(String hashSum) {
        this.hashSum = hashSum;
    }

    public String getHashSum() {
        return hashSum;
    }

    public static enum Suffix {
        INSTANCE;

        private final String[] suffixes = {"rpm", "tarxz", "zip", "msi"};

        public String[] getSuffixes() {
            return Arrays.copyOf(suffixes, suffixes.length);
        }

        public String getSuffix(String url, String filename) {
            if (url==null){
                return getSuffix(filename);
            } else {
                return getSuffix(url);
            }
        }
        public String getSuffix(String url) {
            if (url == null) {
                return "";
            }
            for (String suffix : suffixes) {
                if (url.endsWith(suffix)) {
                    return suffix;
                }
            }
            return "";
        }

        public boolean endsWithSuffix(String url) {
            if (url == null) {
                return false;
            }
            for (String suffix : suffixes) {
                if (url.endsWith(suffix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
