package org.fakekoji.jobmanager.model;

import java.util.Objects;

public class Product {

    private final String jdk;
    private final String packageName;

    public Product() {
        jdk = null;
        packageName = null;
    }

    public Product(String jdk, String packageName) {
        this.jdk = jdk;
        this.packageName = packageName;
    }

    public String getJdk() {
        return jdk;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return Objects.equals(jdk, product.jdk) &&
                Objects.equals(packageName, product.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jdk, packageName);
    }

    @Override
    public String toString() {
        return "Product{" +
                "jdk='" + jdk + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}
