package org.fakekoji.model;

import java.util.Map;
import java.util.Objects;

public class TaskVariantCategory {

    private final String id;
    private final String label;
    private final Usage type;
    private final int order;
    private final Map<String, TaskVariant> variants;

    public TaskVariantCategory() {
        id = null;
        label = null;
        type = null;
        order = -1;
        variants = null;
    }

    public TaskVariantCategory(
            String id,
            String label,
            Usage type,
            int order,
            Map<String, TaskVariant> variants
    ) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.order = order;
        this.variants = variants;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Usage getType() {
        return type;
    }

    public int getOrder() {
        return order;
    }

    public Map<String, TaskVariant> getVariants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskVariantCategory)) return false;
        TaskVariantCategory category = (TaskVariantCategory) o;
        return order == category.order &&
                Objects.equals(id, category.id) &&
                Objects.equals(label, category.label) &&
                type == category.type &&
                Objects.equals(variants, category.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, type, order, variants);
    }

    @Override
    public String toString() {
        return "TaskVariantCategory{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", type=" + type +
                ", order=" + order +
                ", variants=" + variants +
                '}';
    }

    public enum Usage {
        BUILD("BUILD"),
        TEST("TEST");

        private final String value;

        Usage(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
