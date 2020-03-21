package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TaskConfig {

    private final String id;
    private final List<VariantsConfig> variants;

    public TaskConfig() {
        id = null;
        variants = Collections.emptyList();
    }

    public TaskConfig(final String id, List<VariantsConfig> variants) {
        this.id = id;
        this.variants = variants != null ? variants : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public List<VariantsConfig> getVariants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskConfig)) return false;
        TaskConfig that = (TaskConfig) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, variants);
    }
}
