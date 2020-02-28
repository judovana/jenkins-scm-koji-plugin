package org.fakekoji.jobmanager.model;

import org.fakekoji.model.OToolVariable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Project implements Comparable<Project> {

    private final String id;
    private final Product product;
    private final ProjectType type;
    private final Set<String> buildProviders;
    private final List<OToolVariable> variables;

    public Project() {
        id = null;
        product = null;
        type = null;
        buildProviders = null;
        variables = null;
    }

    public Project(
            String id,
            Product product,
            ProjectType type,
            Set<String> buildProviders,
            List<OToolVariable> variables
            ) {
        this.id = id;
        this.product = product;
        this.type = type;
        this.buildProviders = buildProviders;
        this.variables = variables;
    }

    public String getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public ProjectType getType() {
        return type;
    }

    public Set<String> getBuildProviders() {
        return buildProviders;
    }

    public List<OToolVariable> getVariables() {
        return variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id) &&
                Objects.equals(product, project.product) &&
                type == project.type &&
                Objects.equals(buildProviders, project.buildProviders) &&
                Objects.equals(variables, project.getVariables());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, product, type, buildProviders, variables);
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

    @Override
    public int compareTo(@NotNull Project project) {
        return this.getId().compareTo(project.getId());
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
