package org.fakekoji.jobmanager.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class JDKTestProject extends Project {

    private final List<String> subpackageBlacklist;
    private final List<String> subpackageWhitelist;
    private final TestJobConfiguration jobConfiguration;

    public JDKTestProject() {
        this.subpackageBlacklist = null;
        this.subpackageWhitelist = null;
        this.jobConfiguration = null;
    }

    public JDKTestProject(
            String id,
            Product product,
            Set<String> buildProviders,
            List<String> subpackageBlacklist,
            List<String> subpackageWhitelist,
            TestJobConfiguration jobConfiguration
    ) {
        super(id, product, ProjectType.JDK_TEST_PROJECT, buildProviders);
        this.subpackageBlacklist = subpackageBlacklist;
        this.subpackageWhitelist = subpackageWhitelist;
        this.jobConfiguration = jobConfiguration;
    }

    public List<String> getSubpackageBlacklist() {
        return subpackageBlacklist;
    }

    public List<String> getSubpackageWhitelist() {
        return subpackageWhitelist;
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
        return Objects.equals(subpackageBlacklist, that.subpackageBlacklist) &&
                Objects.equals(subpackageWhitelist, that.subpackageWhitelist) &&
                Objects.equals(jobConfiguration, that.jobConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subpackageBlacklist, subpackageWhitelist, jobConfiguration);
    }

    @Override
    public String toString() {
        return "JDKTestProject{" +
                ", subpackageBlacklist='" + subpackageBlacklist + '\'' +
                ", subpackageWhitelist='" + subpackageWhitelist + '\'' +
                ", jobConfiguration=" + jobConfiguration +
                '}';
    }
}
