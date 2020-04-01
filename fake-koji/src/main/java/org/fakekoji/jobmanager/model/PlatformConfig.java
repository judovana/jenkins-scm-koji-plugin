package org.fakekoji.jobmanager.model;

import org.fakekoji.Utils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class PlatformConfig {

    private final String id;
    private final Set<TaskConfig> tasks;
    private final String provider;

    public PlatformConfig() {
        id = null;
        tasks = Collections.emptySet();
        provider = null;
    }

    public PlatformConfig(String id, Set<TaskConfig> tasks, String provider) {
        this.id = id;
        this.tasks = tasks != null ? tasks : Collections.emptySet();
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public Set<TaskConfig> getTasks() {
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
                Utils.areEqual(tasks, that.tasks) &&
                Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tasks, provider);
    }

    @Override
    public String toString() {
        return "PlatformConfig{" +
                "id='" + id + '\'' +
                ", tasks=" + tasks +
                ", provider='" + provider + '\'' +
                '}';
    }
}
