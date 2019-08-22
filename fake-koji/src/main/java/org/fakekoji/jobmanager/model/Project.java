package org.fakekoji.jobmanager.model;

import java.util.Objects;
import java.util.Set;

public class Project {

    private final String id;
    private final String url;
    private final Set<String> buildProviders;
    private final String product;
    private final JobConfiguration jobConfiguration;

    public Project() {
        id = null;
        url = null;
        buildProviders = null;
        product = null;
        jobConfiguration = null;
    }

    public Project(
            String id,
            String url,
            Set<String> buildProviders,
            String product,
            JobConfiguration jobConfiguration
    ) {
        this.id = id;
        this.url = url;
        this.buildProviders = buildProviders;
        this.product = product;
        this.jobConfiguration = jobConfiguration;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Set<String> getBuildProviders() {
        return buildProviders;
    }

    public String getProduct() {
        return product;
    }

    public JobConfiguration getJobConfiguration() {
        return jobConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id) &&
                Objects.equals(url, project.url) &&
                Objects.equals(buildProviders, project.buildProviders) &&
                Objects.equals(product, project.product) &&
                Objects.equals(jobConfiguration, project.jobConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, product, jobConfiguration);
    }

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", url='" + url + '\'' +
                ", buildProviders='" + buildProviders + "\'" +
                ", product='" + product + '\'' +
                ", jobConfiguration=" + jobConfiguration +
                '}';
    }
}
