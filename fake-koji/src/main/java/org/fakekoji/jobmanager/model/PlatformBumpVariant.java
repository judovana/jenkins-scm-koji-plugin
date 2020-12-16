package org.fakekoji.jobmanager.model;

import org.fakekoji.functional.Result;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum PlatformBumpVariant {
    BUILD_ONLY("build_only"),
    TEST_ONLY("test_only"),
    BOTH("both_only");

    public final String value;

    PlatformBumpVariant(final String value) {
        this.value = value;
    }

    public static Result<PlatformBumpVariant, String> parse(final String value) {
        try {
            return Result.ok(PlatformBumpVariant.valueOf(value));
        } catch (IllegalArgumentException e) {

            final String options = Arrays.stream(PlatformBumpVariant.values())
                    .map(variant -> variant.value)
                    .collect(Collectors.joining(", "));
            return Result.err("Invalid platform bump variant: " + value + ". Valid options are: " + options);
        }
    }
}
