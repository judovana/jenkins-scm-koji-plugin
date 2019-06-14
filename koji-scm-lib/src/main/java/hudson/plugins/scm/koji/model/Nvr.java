package hudson.plugins.scm.koji.model;

import hudson.plugins.scm.koji.Constants;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = Constants.nvr)
@XmlAccessorType(XmlAccessType.FIELD)
public class Nvr implements Comparable<Nvr>, java.io.Serializable {

    @XmlElement(name = "name")
    private final String name;
    @XmlElement(name = "version")
    private final String version;
    @XmlElement(name = "release")
    private final String release;
    //Super important!  for sorting in plugin to select latest N!
    @XmlElement(name = "date")
    private final long date;
    @XmlElementWrapper(name = Constants.files)
    @XmlElement(name = Constants.file)
    private final List<String> files;

    public Nvr(String n, String v, String r, long d, List<String> f) {
        name = n;
        version = v;
        release = r;
        files = f;
        date = d;
    }

    @Override
    public String toString() {
        return name + "-" + version + "-" + release + " (" + files.size() + "/" + date + ")";
    }

    @Override
    public int compareTo(Nvr o) {
        if (this.date == o.date) {
            return 0;
        }
        if (this.date < o.date) {
            return -1;
        }
        if (this.date > o.date) {
            return 1;
        }
        //TODO use verson sort as already somewhere in this doc
        //but date should remain much preffered
        int r = name.compareTo(o.name);
        if (r == 0) {
            r = version.compareTo(o.version);
            if (r == 0) {
                r = release.compareTo(o.release);
            }
        }
        return r;
    }
}
