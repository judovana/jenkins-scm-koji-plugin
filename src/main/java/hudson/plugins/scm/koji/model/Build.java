package hudson.plugins.scm.koji.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "build")
@XmlAccessorType(XmlAccessType.FIELD)
public class Build implements Comparable<Build> {

    @XmlElement(name = "id")
    private final Integer id;
    @XmlElement(name = "name")
    private final String name;
    @XmlElement(name = "version")
    private final String version;
    @XmlElement(name = "release")
    private final String release;
    @XmlElement(name = "nvr")
    private final String nvr;
    @XmlElement(name = "completion")
    private final String completionTime;
    @XmlElementWrapper(name = "rpms")
    @XmlElement(name = "rpm")
    private final List<RPM> rpms;
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private final Set<String> tags;

    public Build(Integer id, String name, String version, String release, String nvr, String completionTime,
            List<RPM> rpms, Set<String> tags) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.release = release;
        this.nvr = nvr;
        this.completionTime = completionTime;
        this.rpms = rpms == null ? Collections.emptyList() : new ArrayList<>(rpms);
        this.tags = tags == null ? Collections.emptySet() : new HashSet<>(tags);
    }

    public Build() {
        this.id = null;
        this.name = null;
        this.version = null;
        this.release = null;
        this.nvr = null;
        this.completionTime = null;
        this.rpms = null;
        this.tags = null;
    }

    public Integer getId() {
        return id;
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

    public String getCompletionTime() {
        return completionTime;
    }

    public List<RPM> getRpms() {
        return Collections.unmodifiableList(rpms);
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    @Override
    public String toString() {
        return nvr;
    }

    @Override
    public int compareTo(Build o) {
        if (o == null) {
            return -1;
        }
        if (this == o) {
            return 0;
        }
        int res = compare(this.version, o.version);
        if (res != 0) {
            return res;
        }
        return compare(this.release, o.release);
    }

    public int compare(String o1, String o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return 1;
        }
        // if we are here - o1 is not null:
        if (o2 == null) {
            return -1;
        }
        // if we are here - none of the arguments is null:
        StringTokenizer tokenizer1 = new StringTokenizer(o1, "-.");
        StringTokenizer tokenizer2 = new StringTokenizer(o2, "-.");
        while (tokenizer1.hasMoreTokens() && tokenizer2.hasMoreTokens()) {
            String t1 = tokenizer1.nextToken();
            String t2 = tokenizer2.nextToken();
            if (allDigits(t1) && allDigits(t2)) {
                int i1 = Integer.parseInt(t1);
                int i2 = Integer.parseInt(t2);
                int intCompared = i1 - i2;
                if (intCompared != 0) {
                    return intCompared > 0 ? -1 : 1;
                }
                continue;
            }
            int stringCompared = t1.compareTo(t2);
            if (stringCompared != 0) {
                return stringCompared > 0 ? -1 : 1;
            }
        }
        // if we are here then one of strings has ended,
        // longer will be considered bigger version:
        if (tokenizer1.hasMoreTokens()) {
            return -1;
        }
        if (tokenizer2.hasMoreTokens()) {
            return 1;
        }
        return 0;
    }

    private boolean allDigits(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
