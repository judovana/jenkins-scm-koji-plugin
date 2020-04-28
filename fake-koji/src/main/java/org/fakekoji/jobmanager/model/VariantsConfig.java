package org.fakekoji.jobmanager.model;

import org.fakekoji.Utils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class VariantsConfig {

    private final Map<String, String> map;
    private final Set<PlatformConfig> platforms;

    public VariantsConfig() {
        map = Collections.emptyMap();
        platforms = Collections.emptySet();
    }

    public VariantsConfig(Map<String, String> map) {
        this.map = map != null ? map : Collections.emptyMap();
        this.platforms = Collections.emptySet();
    }

    public VariantsConfig(Map<String, String> map, Set<PlatformConfig> platforms) {
        this.map = map != null ? map : Collections.emptyMap();
        this.platforms = platforms != null ? platforms : Collections.emptySet();
    }

    public Map<String, String> getMap() {
        return map;
    }

    public Set<PlatformConfig> getPlatforms() {
        return platforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariantsConfig)) return false;
        VariantsConfig that = (VariantsConfig) o;
        return Objects.equals(map, that.map) &&
                Utils.areEqual(platforms, that.platforms);
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
