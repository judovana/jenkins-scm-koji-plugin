package org.fakekoji.model;

import java.util.Objects;

public class Product {

    private final String id;
    private final String label;
    private final String version;
    private final String packageName;

    public Product() {
        id = null;
        label = null;
        version = null;
        packageName = null;
    }

    public Product(
            String id,
            String label,
            String version,
            String packageName
    ) {
        this.id = id;
        this.label = label;
        this.version = version;
        this.packageName = packageName;
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

    public String getPackageName() {
        return packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id) &&
                Objects.equals(label, product.label) &&
                Objects.equals(version, product.version) &&
                Objects.equals(packageName, product.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, version, packageName);
    }
}
