package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BuildPlatformConfig {

    private final List<VariantsConfig> variants;

    public BuildPlatformConfig() {
        variants = Collections.emptyList();
    }

    public BuildPlatformConfig(List<VariantsConfig> variants) {
        this.variants = variants != null ? variants : Collections.emptyList();
    }

    public List<VariantsConfig> getVariants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildPlatformConfig)) return false;
        BuildPlatformConfig that = (BuildPlatformConfig) o;
        return Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variants);
    }
}
