package org.fakekoji.jobmanager.model;

import java.util.Objects;
import java.util.Set;

public class JDKProject extends Project {
    private final String url;
    private final Set<String> buildProviders;
    private final String product;
    private final JobConfiguration jobConfiguration;

    public JDKProject() {
        super();
        url = null;
        buildProviders = null;
        product = null;
        jobConfiguration = null;
    }

    public JDKProject(
            String id,
            ProjectType type,
            String url,
            Set<String> buildProviders,
            String product,
            JobConfiguration jobConfiguration
    ) {
        super(id, type);
        this.url = url;
        this.buildProviders = buildProviders;
        this.product = product;
        this.jobConfiguration = jobConfiguration;
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
        if (!(o instanceof JDKProject)) return false;
        if (!super.equals(o)) return false;
        JDKProject that = (JDKProject) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(buildProviders, that.buildProviders) &&
                Objects.equals(product, that.product) &&
                Objects.equals(jobConfiguration, that.jobConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url, buildProviders, product, jobConfiguration);
    }

    @Override
    public String toString() {
        return "JDKProject{" +
                "url='" + url + '\'' +
                ", buildProviders=" + buildProviders +
                ", product='" + product + '\'' +
                ", jobConfiguration=" + jobConfiguration +
                '}';
    }
}
