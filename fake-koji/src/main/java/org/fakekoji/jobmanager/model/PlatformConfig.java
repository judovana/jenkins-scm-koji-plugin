package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PlatformConfig {

    private final String id;
    private final List<TaskConfig> tasks;
    private final String provider;

    public PlatformConfig() {
        id = null;
        tasks = Collections.emptyList();
        provider = null;
    }

    public PlatformConfig(String id, List<TaskConfig> tasks, String provider) {
        this.id = id;
        this.tasks = tasks != null ? tasks : Collections.emptyList();
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public List<TaskConfig> getTasks() {
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
        return Objects.equals(id, that.id) &&
                Objects.equals(tasks, that.tasks) &&
                Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tasks, provider);
    }
}
