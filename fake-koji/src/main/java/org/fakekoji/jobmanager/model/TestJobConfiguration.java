package org.fakekoji.jobmanager.model;

import org.fakekoji.Utils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class TestJobConfiguration {

    private final Set<BuildPlatformConfig> platforms;

    public TestJobConfiguration() {
        platforms = Collections.emptySet();
    }

    public TestJobConfiguration(Set<BuildPlatformConfig> platforms) {
        this.platforms = platforms != null ? platforms : Collections.emptySet();
    }

    public Set<BuildPlatformConfig> getPlatforms() {
        return platforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestJobConfiguration)) return false;
        TestJobConfiguration that = (TestJobConfiguration) o;
        return Utils.areEqual(platforms, that.platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }

    @Override
    public String toString() {
        return "TestJobConfiguration{" +
                "platforms=" + platforms +
                '}';
    }
}
