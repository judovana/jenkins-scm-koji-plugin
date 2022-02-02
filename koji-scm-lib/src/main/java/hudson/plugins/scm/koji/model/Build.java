package hudson.plugins.scm.koji.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.plugins.scm.koji.Constants;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = Constants.build)
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressFBWarnings(value = {"EQ_COMPARETO_USE_OBJECT_EQUALS"}, justification = "is working like this, is serialised to xml, am afraid to fix this")
public class Build implements Comparable<Build>, java.io.Serializable {

    @XmlElement(name = "manual")
    private final Boolean manual;
    @XmlElement(name = "id")
    private final Integer id;
    @XmlElement(name = Constants.name)
    private final String name;
    @XmlElement(name = Constants.version)
    private final String version;
    @XmlElement(name = Constants.release)
    private final String release;
    @XmlElement(name = Constants.nvr)
    private final String nvr;
    @XmlElement(name = "completion")
    private final String completionTime;
    @XmlElementWrapper(name = Constants.rpms)
    @XmlElement(name = "rpm")
    private final List<RPM> rpms;
    @XmlElementWrapper(name = "tags")
    @XmlElement(name = "tag")
    private final Set<String> tags;
    @XmlElement(name = "provider", type = BuildProvider.class)
    private BuildProvider provider;
    private URL srcUrl;

    public Build(
            Integer id,
            String name,
            String version,
            String release,
            String nvr,
            String completionTime,
            List<RPM> rpms,
            Set<String> tags,
            BuildProvider provider,
            Boolean manual) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.release = release;
        this.nvr = nvr;
        this.completionTime = completionTime;
        this.manual = manual;
        this.rpms = rpms == null ? Collections.emptyList() : new ArrayList<>(rpms);
        this.tags = tags == null ? Collections.emptySet() : new HashSet<>(tags);
        this.provider = provider;
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
        this.manual = null;
    }

    public boolean isManual(){
        if (manual == null){
            return false;
        }
        return manual;
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
    public int compareTo(Build build) {
        if (build == null) {
            return -1;
        }
        if (this == build) {
            return 0;
        }
        final String thisCompletionTime = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.parse(getCompletionTime(), Constants.DTF));
        final String thatCompletionTime = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.parse(build.getCompletionTime(), Constants.DTF));
        final LocalDateTime thisCompletionDateTime = LocalDateTime.parse(thisCompletionTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        final LocalDateTime thatCompletionDateTime = LocalDateTime.parse(thatCompletionTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return thatCompletionDateTime.compareTo(thisCompletionDateTime);
    }

    public BuildProvider getProvider() {
        return provider;
    }

    public URL getSrcUrl() {
        return srcUrl;
    }

    public void setSrcUrl(URL srcUrl) {
        this.srcUrl = srcUrl;
    }
}
