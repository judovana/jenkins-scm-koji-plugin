package org.fakekoji.api.http.rest.args;

import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.StorageException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.fakekoji.api.http.rest.RestUtils.extractMandatoryParamValue;

public class AddTaskVariantArgs extends BumpArgs {
    public final TaskVariant taskVariant;

    public AddTaskVariantArgs(final BumpArgs bumpArgs, final TaskVariant taskVariant) {
        super(bumpArgs);
        this.taskVariant = taskVariant;
    }

    private static Result<ExtractedParams, OToolError> extractParams(final Map<String, List<String>> params) {
        return extractMandatoryParamValue(params, "name").flatMap(name ->
                extractMandatoryParamValue(params, "type").flatMap(type ->
                        extractMandatoryParamValue(params, "values").flatMap(values ->
                                extractMandatoryParamValue(params, "defaultValue").flatMap(defaultValue ->
                                        Result.ok(new ExtractedParams(name, type, values, defaultValue))
                                ))));
    }


    public static Result<AddTaskVariantArgs, OToolError> parse(
            final ConfigManager configManager,
            final Map<String, List<String>> params
    ) {
        return parseBumpArgs(params).flatMap(bumpArgs -> extractParams(params).flatMap(extParams -> {
            final Result<Task.Type, String> typeParseResult = Task.Type.parse(extParams.type);
            if (typeParseResult.isError()) {
                return Result.err(new OToolError(typeParseResult.getError(), 400));
            }
            final Task.Type taskType = typeParseResult.getValue();
            final Collection<TaskVariant> taskVariants;
            final int order;
            try {
                taskVariants = configManager.getTaskVariantManager().readAll();
                order = taskVariants.stream()
                        .filter(taskVariant -> taskVariant.getType().equals(taskType))
                        .max(Comparator.comparingInt(TaskVariant::getOrder))
                        .orElseThrow(() -> new StorageException("Error while getting last taskVariant's order"))
                        .getOrder() + 1;
            } catch (StorageException e) {
                return Result.err(new OToolError(e.getMessage(), 500));
            }
            final Set<String> taskVariantValuesSet = taskVariants.stream()
                    .flatMap(taskVariant -> taskVariant.getVariants().keySet().stream())
                    .collect(Collectors.toSet());
            final List<String> valuesList = Arrays
                    .stream(extParams.values.split(","))
                    .collect(Collectors.toList());
            final Set<String> tmp = new HashSet<>();
            for (final String value : valuesList) {
                if (taskVariantValuesSet.contains(value)) {
                    return Result.err(new OToolError("Value " + value + " already exists in another task variant", 400));
                }
                if (!tmp.add(value)) {
                    return Result.err(new OToolError("Duplicate value: " + value, 400));
                }
            }
            final Map<String, TaskVariantValue> taskVariantValues = valuesList.stream()
                    .collect(Collectors.toMap(value -> value, value -> new TaskVariantValue(value, value)));
            if (!taskVariantValues.containsKey(extParams.defaultValue)) {
                return Result.err(new OToolError(
                        "Default value '" + extParams.defaultValue + "' is not defined in values",
                        500
                ));
            }
            final TaskVariant taskVariant = new TaskVariant(
                    extParams.name,
                    extParams.name,
                    taskType,
                    extParams.defaultValue,
                    order,
                    taskVariantValues,
                    false
            );
            return Result.ok(new AddTaskVariantArgs(bumpArgs, taskVariant));
        }));
    }

    private static class ExtractedParams {
        private final String name;
        private final String type;
        private final String values;
        private final String defaultValue;

        ExtractedParams(
                final String name,
                final String type,
                final String values,
                final String defaultValue
        ) {
            this.name = name;
            this.type = type;
            this.values = values;
            this.defaultValue = defaultValue;
        }
    }
}
