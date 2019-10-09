package org.fakekoji.model;

import java.util.Map;
import java.util.Objects;

public class TaskVariant implements Comparable<TaskVariant> {

    private final String id;
    private final String label;
    private final Task.Type type;
    private final int order;
    private final Map<String, TaskVariantValue> variants;

    public TaskVariant() {
        id = null;
        label = null;
        type = null;
        order = -1;
        variants = null;
    }

    public TaskVariant(
            String id,
            String label,
            Task.Type type,
            int order,
            Map<String, TaskVariantValue> variants
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

    public Task.Type getType() {
        return type;
    }

    public int getOrder() {
        return order;
    }

    public Map<String, TaskVariantValue> getVariants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskVariant)) return false;
        TaskVariant category = (TaskVariant) o;
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
        return "TaskVariant{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", type=" + type +
                ", order=" + order +
                ", variants=" + variants +
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
