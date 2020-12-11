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

    public static Result<JobCollisionAction, String> parse(final String value) {
        try {
            return Result.ok(JobCollisionAction.valueOf(value));
        } catch (IllegalArgumentException e) {

            final String options = Arrays.stream(JobCollisionAction.values())
                    .map(action -> action.value)
                    .collect(Collectors.joining(", "));
            return Result.err("Invalid job collision action: " + value + ". Valid options are: " + options);
        }
    }
}

