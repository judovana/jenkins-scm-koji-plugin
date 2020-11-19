package org.fakekoji.api.http.rest.args;

import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.functional.Result;

import java.util.List;
import java.util.Map;

import static org.fakekoji.api.http.rest.RestUtils.extractMandatoryParamValue;

public class RemoveTaskVariantArgs {
    public final String name;

    RemoveTaskVariantArgs(final String name) {
        this.name = name;
    }

    public static Result<RemoveTaskVariantArgs, OToolError> parse(final Map<String, List<String>> params) {
        return extractMandatoryParamValue(params, "name").flatMap(name ->
                Result.ok(new RemoveTaskVariantArgs(name))
        );
    }
}
