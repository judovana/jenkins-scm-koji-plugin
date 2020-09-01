package org.fakekoji.model;

import java.util.Objects;

public class BuildProvider {

    private final String id;
    private final String label;
    private final String topUrl;
    private final String downloadUrl;
    private final String packageInfoUrl;

    public BuildProvider() {
        id = null;
        label = null;
        topUrl = null;
        downloadUrl = null;
        packageInfoUrl = null;
    }

    public BuildProvider(
            String id,
            String label,
            String topUrl,
            String downloadUrl,
            final String packageInfoUrl
    ) {
        this.id = id;
        this.label = label;
        this.topUrl = topUrl;
        this.downloadUrl = downloadUrl;
        this.packageInfoUrl = packageInfoUrl;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getTopUrl() {
        return topUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getPackageInfoUrl() {
        return packageInfoUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildProvider)) return false;
        BuildProvider that = (BuildProvider) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(label, that.label) &&
                Objects.equals(topUrl, that.topUrl) &&
                Objects.equals(downloadUrl, that.downloadUrl) &&
                Objects.equals(packageInfoUrl, that.packageInfoUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, topUrl, downloadUrl, packageInfoUrl);
    }

    @Override
    public String toString() {
        return "BuildProvider{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", topUrl='" + topUrl + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", packageInfoUrl='" + packageInfoUrl + '\'' +
                '}';
    }
}
