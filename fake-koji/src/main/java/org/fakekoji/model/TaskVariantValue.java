package org.fakekoji.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TaskVariantValue {

    private final String id;
    private final String label;
    private final List<String> subpackageBlacklist;
    private final List<String> subpackageWhitelist;

    public TaskVariantValue() {
        id = null;
        label = null;
        subpackageBlacklist = null;
        subpackageWhitelist = null;
    }

    public TaskVariantValue(String id, String label) {
        this.id = id;
        this.label = label;
        subpackageBlacklist = null;
        subpackageWhitelist = null;
    }

    public TaskVariantValue(
            String id,
            String label,
            List<String> subpackageBlacklist,
            List<String> subpackageWhitelist
    ) {
        this.id = id;
        this.label = label;
        this.subpackageBlacklist = subpackageBlacklist;
        this.subpackageWhitelist = subpackageWhitelist;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getSubpackageBlacklist() {
        return subpackageBlacklist == null ? Collections.emptyList() : subpackageBlacklist;
    }

    public List<String> getSubpackageWhitelist() {
        return subpackageWhitelist == null ? Collections.emptyList() : subpackageWhitelist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskVariantValue)) return false;
        TaskVariantValue that = (TaskVariantValue) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(label, that.label) &&
                Objects.equals(subpackageBlacklist, that.subpackageBlacklist) &&
                Objects.equals(subpackageWhitelist, that.subpackageWhitelist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, subpackageBlacklist, subpackageWhitelist);
    }

    @Override
    public String toString() {
        return "TaskVariantValue{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", subpackageBlacklist=" + subpackageBlacklist +
                ", subpackageWhitelist=" + subpackageWhitelist +
                '}';
    }
}
