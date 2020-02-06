package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class PlatformConfig {

    private final Map<String, TaskConfig> tasks;
    private final String provider;

    public PlatformConfig() {
        tasks = Collections.emptyMap();
        provider = null;
    }

    public PlatformConfig(Map<String, TaskConfig> tasks, String provider) {
        this.tasks = tasks != null ? tasks : Collections.emptyMap();
        this.provider = provider;
    }

    public Map<String, TaskConfig> getTasks() {
        return tasks;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlatformConfig)) return false;
        PlatformConfig that = (PlatformConfig) o;
        return Objects.equals(tasks, that.tasks) &&
                Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tasks, provider);
    }
}
