package org.fakekoji.jobmanager.model;

import java.util.Objects;
import java.util.Set;

public class JDKProject extends Project {
    private final String url;
    private final RepoState repoState;
    private final Set<String> buildProviders;
    private final String product;
    private final JobConfiguration jobConfiguration;

    public JDKProject() {
        super();
        url = null;
        repoState = null;
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
        this(
                id,
                type,
                RepoState.NOT_CLONED,
                url,
                buildProviders,
                product,
                jobConfiguration
        );
    }

    public JDKProject(
            String id,
            ProjectType type,
            RepoState repoState,
            String url,
            Set<String> buildProviders,
            String product,
            JobConfiguration jobConfiguration
    ) {
        super(id, type);
        this.url = url;
        this.repoState = repoState;
        this.buildProviders = buildProviders;
        this.product = product;
        this.jobConfiguration = jobConfiguration;
    }

    public String getUrl() {
        return url;
    }

    public RepoState getRepoState() {
        return repoState;
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
                Objects.equals(repoState, that.repoState) &&
                Objects.equals(buildProviders, that.buildProviders) &&
                Objects.equals(product, that.product) &&
                Objects.equals(jobConfiguration, that.jobConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url, repoState, buildProviders, product, jobConfiguration);
    }

    @Override
    public String toString() {
        return "JDKProject{" +
                "url='" + url + '\'' +
                "repoState='" + repoState + '\'' +
                ", buildProviders=" + buildProviders +
                ", product='" + product + '\'' +
                ", jobConfiguration=" + jobConfiguration +
                '}';
    }

    public enum RepoState {
        NOT_CLONED("NOT_CLONED"),
        CLONED("CLONED"),
        CLONE_ERROR("CLOENE_ERROR"),
        CLONING("CLONING");

        private final String value;

        RepoState(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
