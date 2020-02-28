package org.fakekoji.jobmanager.model;

import org.fakekoji.model.OToolVariable;

import java.util.Objects;
import java.util.Set;
import java.util.List;

public class JDKProject extends Project {
    private final String url;
    private final RepoState repoState;
    private final JobConfiguration jobConfiguration;

    public JDKProject() {
        super();
        url = null;
        repoState = null;
        jobConfiguration = null;
    }

    public JDKProject(
            String id,
            Product product,
            String url,
            Set<String> buildProviders,
            JobConfiguration jobConfiguration,
            List<OToolVariable> variables
    ) {
        this(
                id,
                product,
                RepoState.NOT_CLONED,
                url,
                buildProviders,
                jobConfiguration,
                variables
        );
    }

    public JDKProject(
            String id,
            Product product,
            RepoState repoState,
            String url,
            Set<String> buildProviders,
            JobConfiguration jobConfiguration,
            List<OToolVariable> variables
    ) {
        super(id, product, ProjectType.JDK_PROJECT, buildProviders, variables);
        this.url = url;
        this.repoState = repoState;
        this.jobConfiguration = jobConfiguration;
    }

    public String getUrl() {
        return url;
    }

    public RepoState getRepoState() {
        return repoState;
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
                Objects.equals(jobConfiguration, that.jobConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url, repoState, jobConfiguration);
    }

    @Override
    public String toString() {
        return "JDKProject{" +
                "url='" + url + '\'' +
                "repoState='" + repoState + '\'' +
                ", jobConfiguration=" + jobConfiguration +
                '}';
    }

    public enum RepoState {
        NOT_CLONED("NOT_CLONED"),
        CLONED("CLONED"),
        CLONE_ERROR("CLONE_ERROR"),
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
