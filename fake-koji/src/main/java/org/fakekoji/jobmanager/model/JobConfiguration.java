package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class JobConfiguration {

    private final Map<String, PlatformConfig> platforms;

    public JobConfiguration() {
        platforms = Collections.emptyMap();
    }

    public JobConfiguration(Map<String, PlatformConfig> platforms) {
        this.platforms = platforms != null ? platforms : Collections.emptyMap();
    }

    public Map<String, PlatformConfig> getPlatforms() {
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
