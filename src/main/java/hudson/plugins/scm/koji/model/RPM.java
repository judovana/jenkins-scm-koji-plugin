package hudson.plugins.scm.koji.model;

import hudson.plugins.scm.koji.Constants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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

    public RPM(String name, String version, String release, String nvr, String arch, String filename) {
        this.name = name;
        this.version = version;
        this.release = release;
        this.nvr = nvr;
        this.arch = arch;
        this.filename = filename;
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
        return nvr + '.' + arch + ".suffix";
    }

    public String getFilename(String suffix) {
        return Optional.ofNullable(filename).orElse(nvr + '.' + arch + "." + suffix);
    }

}
