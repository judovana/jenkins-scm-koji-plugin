package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.Map;

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
}
