package hudson.plugins.scm.koji.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class BuildProvider implements java.io.Serializable {

    @XmlElement(name = "topUrl")
    private final String topUrl;

    @XmlElement(name = "downloadUrl")
    private final String downloadUrl;

    public BuildProvider() {
        topUrl = null;
        downloadUrl = null;
    }

    public BuildProvider(String topUrl, String downloadUrl) {
        this.topUrl = topUrl;
        this.downloadUrl = downloadUrl;
    }

    public String getTopUrl() {
        return topUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
