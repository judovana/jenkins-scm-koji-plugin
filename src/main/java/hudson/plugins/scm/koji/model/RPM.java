package hudson.plugins.scm.koji.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RPM implements java.io.Serializable {

    @XmlElement(name = "name")
    private final String name;
    @XmlElement(name = "version")
    private final String version;
    @XmlElement(name = "release")
    private final String release;
    @XmlElement(name = "nvr")
    private final String nvr;
    @XmlElement(name = "arch")
    private final String arch;

    public RPM(String name, String version, String release, String nvr, String arch) {
        this.name = name;
        this.version = version;
        this.release = release;
        this.nvr = nvr;
        this.arch = arch;
    }

    public RPM() {
        this.name = null;
        this.version = null;
        this.release = null;
        this.nvr = null;
        this.arch = null;
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
        return nvr + '.' + arch + ".suffix";
    }

    public String getFilename(String suffix) {
        return nvr + '.' + arch + "." + suffix;
    }

}
