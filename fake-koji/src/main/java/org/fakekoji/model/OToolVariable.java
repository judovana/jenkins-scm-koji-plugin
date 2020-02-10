package org.fakekoji.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OToolVariable {
    private final String comment;
    private final boolean commentedOut;
    private final boolean exported;
    private final boolean defaultPrefix;
    private final String name;
    private final String value;

    public OToolVariable() {
        comment = null;
        commentedOut = false;
        exported = false;
        defaultPrefix = false;
        name = null;
        value = null;
    }

    public OToolVariable(
            String name,
            String value,
            String comment,
            boolean defaultPrefix,
            boolean commented_out,
            boolean exported) {
        this.commentedOut = commented_out;
        this.comment = comment;
        this.exported = exported;
        this.defaultPrefix = defaultPrefix;
        this.name = name;
        this.value = value;
    }

    public OToolVariable(String name, String value) {
        commentedOut = false;
        comment = null;
        exported = true;
        defaultPrefix = true;
        this.name = name;
        this.value = value;
    }

    public static List<OToolVariable> createDefault(Map<TaskVariant, TaskVariantValue> map) {
        return map.entrySet()
                .stream()
                .map(entry -> new OToolVariable(entry.getKey().getId(), entry.getValue().getId()))
                .collect(Collectors.toList());
    }

    public boolean isCommentedOut() {
        return commentedOut;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public boolean isExported() {
        return exported;
    }

    public boolean isDefaultPrefix() {
        return defaultPrefix;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
