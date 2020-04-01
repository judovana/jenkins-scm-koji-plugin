package org.fakekoji.jobmanager.model;

import org.fakekoji.Utils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class TaskConfig {

    private final String id;
    private final Set<VariantsConfig> variants;

    public TaskConfig() {
        id = null;
        variants = Collections.emptySet();
    }

    public TaskConfig(final String id, Set<VariantsConfig> variants) {
        this.id = id;
        this.variants = variants != null ? variants : Collections.emptySet();
    }

    public String getId() {
        return id;
    }

    public Set<VariantsConfig> getVariants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskConfig)) return false;
        TaskConfig that = (TaskConfig) o;
        return Objects.equals(id, that.id) &&
                Utils.areEqual(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, variants);
    }

    @Override
    public String toString() {
        return "TaskConfig{" +
                "id='" + id + '\'' +
                ", variants=" + variants +
                '}';
    }
}
