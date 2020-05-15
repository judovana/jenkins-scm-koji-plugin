package org.fakekoji.api.http.rest;

import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.model.Product;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class RestUtils {

    static Optional<String> extractParamValue(Map<String, List<String>> paramsMap, String param) {
        return Optional.ofNullable(paramsMap.get(param))
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0));
    }

    static Result<String, OToolError> extractMandatoryParamValue(Map<String, List<String>> paramsMap, String param) {
        return extractParamValue(paramsMap, param).<Result<String, OToolError>>map(Result::ok)
                .orElseGet(() -> Result.err(new OToolError(
                        "Missing mandatory parameter: '" + param + "'!",
                        400
                )));
    }

    static Result<Tuple<Product, Product>, OToolError> extractProducts(final Map<String, List<String>> paramsMap) {
        return extractMandatoryParamValue(paramsMap, "from").flatMap(fromValue ->
           extractProduct(fromValue).flatMap(fromProduct ->
               extractMandatoryParamValue(paramsMap, "to").flatMap(toValue ->
                   extractProduct(toValue).flatMap(toProduct ->
                       Result.ok(new Tuple<>(fromProduct, toProduct))
                   )
               )
           )
        );
    }

    static Result<Product, OToolError> extractProduct(final String paramValue) {
        final String[] split = paramValue.split(",");
        if (split.length != 2 || split[0].trim().isEmpty() || split[1].trim().isEmpty()) {
            return Result.err(new OToolError("Expected format jdkVersionId,packageName", 400));
        }
        return Result.ok(new Product(split[0], split[1]));
    }

    static Result<List<String>, OToolError> extractProjectIds(Map<String, List<String>> paramsMap) {
        final Optional<String> projectIdsOptional = extractParamValue(paramsMap, "projects");
        return projectIdsOptional.<Result<List<String>, OToolError>>map(s ->
                Result.ok(Arrays.asList(s.split(",")))
        ).orElseGet(() -> Result.err(new OToolError(
                "projects are mandatory. Use get/projects?as=list to get them all",
                400
        )));
    }
}
