package org.fakekoji.jobmanager.model;

import org.fakekoji.Utils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class JobConfiguration {

    private final Set<PlatformConfig> platforms;

    public JobConfiguration() {
        platforms = Collections.emptySet();
    }

    public JobConfiguration(Set<PlatformConfig> platforms) {
        this.platforms = platforms != null ? platforms : Collections.emptySet();
    }

    public Set<PlatformConfig> getPlatforms() {
        return platforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobConfiguration)) return false;
        JobConfiguration that = (JobConfiguration) o;
        return Utils.areEqual(platforms, that.platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }

    @Override
    public String toString() {
        return "JobConfiguration{" +
                "platforms=" + platforms +
                '}';
    }
}
