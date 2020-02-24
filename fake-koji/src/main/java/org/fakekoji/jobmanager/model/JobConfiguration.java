package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JobConfiguration {

    private final List<PlatformConfig> platforms;

    public JobConfiguration() {
        platforms = Collections.emptyList();
    }

    public JobConfiguration(List<PlatformConfig> platforms) {
        this.platforms = platforms != null ? platforms : Collections.emptyList();
    }

    public List<PlatformConfig> getPlatforms() {
        return platforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobConfiguration)) return false;
        JobConfiguration that = (JobConfiguration) o;
        return Objects.equals(platforms, that.platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platforms);
    }

}
