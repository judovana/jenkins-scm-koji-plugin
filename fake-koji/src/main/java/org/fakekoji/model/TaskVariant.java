package org.fakekoji.model;

import java.util.Map;
import java.util.Objects;

public class TaskVariant implements Comparable<TaskVariant> {

    private final String id;
    private final String label;
    private final Task.Type type;
    private final String defaultValue;
    private final int order;
    private final Map<String, TaskVariantValue> variants;
    private final boolean supportsSubpackages;

    public TaskVariant() {
        id = null;
        label = null;
        type = null;
        defaultValue = null;
        order = -1;
        variants = null;
        supportsSubpackages = false;
    }

    public TaskVariant(
            String id,
            String label,
            Task.Type type,
            String defaultValue,
            int order,
            Map<String, TaskVariantValue> variants,
            boolean supportsSubpackageSystem
    ) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.order = order;
        this.variants = variants;
        this.supportsSubpackages = supportsSubpackageSystem;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Task.Type getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public int getOrder() {
        return order;
    }

    public Map<String, TaskVariantValue> getVariants() {
        return variants;
    }

    public boolean isSupportsSubpackages() {
        return supportsSubpackages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskVariant)) return false;
        TaskVariant that = (TaskVariant) o;
        return order == that.order &&
                supportsSubpackages == that.supportsSubpackages &&
                Objects.equals(id, that.id) &&
                Objects.equals(label, that.label) &&
                type == that.type &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, type, defaultValue, order, variants, supportsSubpackages);
    }

    @Override
    public String toString() {
        return "TaskVariant{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", type=" + type +
                ", defaultValud=" + defaultValue +
                ", order=" + order +
                ", variants=" + variants +
                ", supportsSubpackages=" + supportsSubpackages +
                '}';
    }

    @Override
    public int compareTo(TaskVariant taskVariant) {
        if (type == null || taskVariant == null || taskVariant.type == null) {
            return 0;
        }
        if (type.getOrder() == taskVariant.type.getOrder()) {
            return order - taskVariant.order;
        }
        return type.getOrder() - taskVariant.type.getOrder();
    }
}
