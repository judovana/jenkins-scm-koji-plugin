package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TestJobConfiguration {

    private final List<BuildPlatformConfig> platforms;

    public TestJobConfiguration() {
        platforms = Collections.emptyList();
    }

    public TestJobConfiguration(List<BuildPlatformConfig> platforms) {
        this.platforms = platforms != null ? platforms : Collections.emptyList();
    }

    public List<BuildPlatformConfig> getPlatforms() {
        return platforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestJobConfiguration)) return false;
        TestJobConfiguration that = (TestJobConfiguration) o;
        return Objects.equals(platforms, that.platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }
}
