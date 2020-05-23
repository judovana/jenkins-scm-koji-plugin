package org.fakekoji.jobmanager.model;

import org.fakekoji.Utils;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.model.TaskVariantValue;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public String concatVariants(final ConfigCache cache) {
        return map.entrySet()
                .stream()
                .map(entry -> {
                    final String variantId = entry.getKey();
                    final String valueId = entry.getValue();
                    return cache.getTaskVariant(variantId).map(variant -> {
                        final TaskVariantValue value = variant.getVariants().get(valueId);
                        return new Tuple<>(variant, value);
                    });
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(tuple -> tuple.x))
                .map(tuple -> tuple.y.getId())
                .collect(Collectors.joining("."));
    }
}
