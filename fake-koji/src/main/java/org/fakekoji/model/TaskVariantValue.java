package org.fakekoji.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TaskVariantValue {

    private final String id;
    private final String label;
    private final List<String> subpackageDenylist;
    private final List<String> subpackageAllowlist;

    public TaskVariantValue() {
        id = null;
        label = null;
        subpackageDenylist = null;
        subpackageAllowlist = null;
    }

    public TaskVariantValue(String id, String label) {
        this.id = id;
        this.label = label;
        subpackageDenylist = null;
        subpackageAllowlist = null;
    }

    public TaskVariantValue(
            String id,
            String label,
            List<String> subpackageDenylist,
            List<String> subpackageAllowlist
    ) {
        this.id = id;
        this.label = label;
        this.subpackageDenylist = subpackageDenylist;
        this.subpackageAllowlist = subpackageAllowlist;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Optional<List<String>> getSubpackageDenylist() {
        return Optional.ofNullable(subpackageDenylist);
    }

    public Optional<List<String>> getSubpackageAllowlist() {
        return Optional.ofNullable(subpackageAllowlist);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskVariantValue)) return false;
        TaskVariantValue that = (TaskVariantValue) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(label, that.label) &&
                Objects.equals(subpackageDenylist, that.subpackageDenylist) &&
                Objects.equals(subpackageAllowlist, that.subpackageAllowlist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, subpackageDenylist, subpackageAllowlist);
    }

    @Override
    public String toString() {
        return "TaskVariantValue{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", subpackageDenylist=" + subpackageDenylist +
                ", subpackageAllowlist=" + subpackageAllowlist +
                '}';
    }
}
