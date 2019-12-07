package org.fakekoji.jobmanager.model;

import java.util.Objects;
import java.util.Set;

public class Project {

    private final String id;
    private final String product;
    private final ProjectType type;
    private final Set<String> buildProviders;

    public Project() {
        id = null;
        product = null;
        type = null;
        buildProviders = null;
    }

    public Project(
            String id,
            String product,
            ProjectType type,
            Set<String> buildProviders
            ) {
        this.id = id;
        this.product = product;
        this.type = type;
        this.buildProviders = buildProviders;
    }

    public String getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public ProjectType getType() {
        return type;
    }

    public Set<String> getBuildProviders() {
        return buildProviders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id) &&
                Objects.equals(product, project.product) &&
                type == project.type &&
                Objects.equals(buildProviders, project.buildProviders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, product, type, buildProviders);
    }

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", product='" + product + '\'' +
                ", type=" + type +
                ", buildProviders=" + buildProviders +
                '}';
    }

    public enum ProjectType {
        JDK_PROJECT("JDK_PROJECT"),
        JDK_TEST_PROJECT("JKD_TEST_PROJECT");

        private final String value;

        ProjectType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
