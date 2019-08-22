package org.fakekoji.model;

import java.util.Objects;

public class TaskVariant {

    private final String id;
    private final String label;

    public TaskVariant() {
        id = null;
        label = null;
    }

    public TaskVariant(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskVariant)) return false;
        TaskVariant that = (TaskVariant) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label);
    }

    @Override
    public String toString() {
        return "TaskVariant{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
