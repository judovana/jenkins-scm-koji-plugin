package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class PlatformConfig {

    private final Map<String, TaskConfig> tasks;

    public PlatformConfig() {
        tasks = Collections.emptyMap();
    }

    public PlatformConfig(Map<String, TaskConfig> tasks) {
        this.tasks = tasks != null ? tasks : Collections.emptyMap();
    }

    public Map<String, TaskConfig> getTasks() {
        return tasks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlatformConfig)) return false;
        PlatformConfig that = (PlatformConfig) o;
        return Objects.equals(tasks, that.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tasks);
    }
}
