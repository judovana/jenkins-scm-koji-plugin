package org.fakekoji.jobmanager.model;

import org.fakekoji.model.OToolVariable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class JDKTestProject extends Project {

    private final List<String> subpackageDenylist;
    private final List<String> subpackageAllowlist;
    private final TestJobConfiguration jobConfiguration;

    public JDKTestProject() {
        this.subpackageDenylist = null;
        this.subpackageAllowlist = null;
        this.jobConfiguration = null;
    }

    public JDKTestProject(
            String id,
            Product product,
            Set<String> buildProviders,
            List<String> subpackageDenylist,
            List<String> subpackageAllowlist,
            TestJobConfiguration jobConfiguration,
            List<OToolVariable> variables
    ) {
        super(id, product, ProjectType.JDK_TEST_PROJECT, buildProviders, variables);
        this.subpackageDenylist = subpackageDenylist;
        this.subpackageAllowlist = subpackageAllowlist;
        this.jobConfiguration = jobConfiguration;
    }

    public List<String> getSubpackageDenylist() {
        return subpackageDenylist;
    }

    public List<String> getSubpackageAllowlist() {
        return subpackageAllowlist;
    }

    public TestJobConfiguration getJobConfiguration() {
        return jobConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JDKTestProject)) return false;
        if (!super.equals(o)) return false;
        JDKTestProject that = (JDKTestProject) o;
        return Objects.equals(subpackageDenylist, that.subpackageDenylist) &&
                Objects.equals(subpackageAllowlist, that.subpackageAllowlist) &&
                Objects.equals(jobConfiguration, that.jobConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subpackageDenylist, subpackageAllowlist, jobConfiguration);
    }

    @Override
    public String toString() {
        return "JDKTestProject{" +
                ", subpackageDenylist='" + subpackageDenylist + '\'' +
                ", subpackageAllowlist='" + subpackageAllowlist + '\'' +
                ", jobConfiguration=" + jobConfiguration +
                '}';
    }
}
