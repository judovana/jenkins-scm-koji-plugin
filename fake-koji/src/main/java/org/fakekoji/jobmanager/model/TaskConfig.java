package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TaskConfig {

    private final List<VariantsConfig> variants;

    public TaskConfig() {
        variants = Collections.emptyList();
    }

    public TaskConfig(List<VariantsConfig> variants) {
        this.variants = variants != null ? variants : Collections.emptyList();
    }

    public List<VariantsConfig> getVariants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskConfig)) return false;
        TaskConfig that = (TaskConfig) o;
        return Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variants);
    }
}
