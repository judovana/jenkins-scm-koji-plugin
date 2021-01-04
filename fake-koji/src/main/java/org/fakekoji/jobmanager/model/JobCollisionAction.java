package org.fakekoji.jobmanager.model;

import org.fakekoji.functional.Result;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum JobCollisionAction {
    STOP("stop"),
    KEEP_EXISTING("keep_existing"),
    KEEP_BUMPED("keep_bumped");

    public final String value;

    JobCollisionAction(final String value) {
        this.value = value;
    }

    public static String stringValues(final String delimiter) {
        return Arrays.stream(JobCollisionAction.values())
                .map(variant -> variant.value)
                .collect(Collectors.joining(delimiter));
    }

    public static Result<JobCollisionAction, String> parse(final String value) {
        try {
            return Result.ok(JobCollisionAction.valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.err("Invalid job collision action: " + value + ". Valid options are: " + stringValues(", "));
        }
    }
}

