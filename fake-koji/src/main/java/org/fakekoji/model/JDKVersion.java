package org.fakekoji.model;

import java.util.List;
import java.util.Objects;

public class JDKVersion {

    private final String id;
    private final String label;
    private final String version;
    private final List<String> packageNames;

    public JDKVersion() {
        id = null;
        label = null;
        version = null;
        packageNames = null;
    }

    public JDKVersion(
            String id,
            String label,
            String version,
            List<String> packageNames
    ) {
        this.id = id;
        this.label = label;
        this.version = version;
        this.packageNames = packageNames;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getPackageNames() {
        return packageNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JDKVersion)) return false;
        JDKVersion product = (JDKVersion) o;
        return Objects.equals(id, product.id) &&
                Objects.equals(label, product.label) &&
                Objects.equals(version, product.version) &&
                Objects.equals(packageNames, product.packageNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, version, packageNames);
    }
}
