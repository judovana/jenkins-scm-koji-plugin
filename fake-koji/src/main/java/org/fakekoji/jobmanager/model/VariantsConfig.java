package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VariantsConfig {

    private final Map<String, String> map;
    private final List<PlatformConfig> platforms;

    public VariantsConfig() {
        map = Collections.emptyMap();
        platforms = Collections.emptyList();
    }

    public VariantsConfig(Map<String, String> map) {
        this.map = map != null ? map : Collections.emptyMap();
        this.platforms = Collections.emptyList();
    }

    public VariantsConfig(Map<String, String> map, List<PlatformConfig> platforms) {
        this.map = map != null ? map : Collections.emptyMap();
        this.platforms = platforms != null ? platforms : Collections.emptyList();
    }

    public Map<String, String> getMap() {
        return map;
    }

    public List<PlatformConfig> getPlatforms() {
        return platforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariantsConfig)) return false;
        VariantsConfig that = (VariantsConfig) o;
        return Objects.equals(map, that.map) &&
                Objects.equals(platforms, that.platforms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, platforms);
    }

    @Override
    public String toString() {
        return "VariantsConfig{" +
                "map=" + map +
                ", platforms=" + platforms +
                '}';
    }
}
