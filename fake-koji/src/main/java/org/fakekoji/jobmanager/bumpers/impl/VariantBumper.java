package org.fakekoji.jobmanager.bumpers.impl;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.bumpers.JobModifier;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VariantBumper extends JobModifier {

    private final String from;
    private final String to;
    private final Pattern filter;
    private final List<TaskVariant> variants;
    private final TaskVariant sharedParent;


    public VariantBumper(
            final AccessibleSettings settings,
            final String from,
            final String to,
            final Pattern filter
    ) {
        super(settings);
        this.from = from;
        this.to = to;
        this.filter = filter;
        try {
            this.variants = settings.getConfigManager().taskVariantManager.readAll();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        TaskVriantWithValue fromVariant = getVariantByValue(from);
        TaskVriantWithValue toVariant = getVariantByValue(to);
        if (!fromVariant.tv.equals(toVariant.tv)) {
            throw new RuntimeException("Variants must be from same group, are not - " + from + "/" + to + " " + fromVariant.tv.getId() + "/" + toVariant.tv.getId());
        }
        sharedParent = fromVariant.tv;
    }

    @Override
    protected boolean shouldPass(BuildJob job) {
        return job.getVariants().values().stream().map(a -> a.getId()).collect(Collectors.toList()).contains(from) && matches(job.getName());
    }

    @Override
    protected boolean shouldPass(TestJob job) {
        return (
                job.getVariants().values().stream().map(a -> a.getId()).collect(Collectors.toList()).contains(from)
                        || job.getBuildVariants().values().stream().map(a -> a.getId()).collect(Collectors.toList()).contains(from)
        ) && matches(job.getName());
    }

    private boolean matches(String name) {
        return filter.matcher(name).matches();
    }

    @Override
    protected BuildJob transform(BuildJob job) {
        return new BuildJob(
                job.getPlatformProvider(),
                job.getProjectName(),
                job.getProduct(),
                job.getJdkVersion(),
                job.getBuildProviders(),
                job.getTask(),
                job.getPlatform(),
                (sharedParent.getType().equals(Task.Type.BUILD))?modifyVariant(job.getVariants(), from, to):job.getVariants(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    @Override
    protected TestJob transform(TestJob job) {
        return new TestJob(
                job.getPlatformProvider(),
                job.getProjectName(),
                job.getProjectType(),
                job.getProduct(),
                job.getJdkVersion(),
                job.getBuildProviders(),
                job.getTask(),
                job.getPlatform(),
                (sharedParent.getType().equals(Task.Type.TEST))?modifyVariant(job.getVariants(), from, to):job.getVariants(),
                job.getBuildPlatform(),
                job.getBuildPlatformProvider(),
                job.getBuildTask(),
                (sharedParent.getType().equals(Task.Type.BUILD))?modifyVariant(job.getBuildVariants(), from, to):job.getBuildVariants(),
                job.getProjectSubpackageBlacklist(),
                job.getProjectSubpackageWhitelist(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    private Map<TaskVariant, TaskVariantValue> modifyVariant(Map<TaskVariant, TaskVariantValue> origs, String from, String to) {
        Map<TaskVariant, TaskVariantValue> result = new HashMap<>(origs.size());
        for (Map.Entry<TaskVariant, TaskVariantValue> orig : origs.entrySet()) {
            if (orig.getValue().getId().equals(from)) {
                result.put(orig.getKey(), getVariantByValue(to).tvv);
            } else {
                result.put(orig.getKey(), orig.getValue());
            }
        }
        return result;
    }

    private static class TaskVriantWithValue {
        private final TaskVariant tv;
        private final TaskVariantValue tvv;

        public TaskVriantWithValue(TaskVariant tv, TaskVariantValue tvv) {
            this.tv = tv;
            this.tvv = tvv;
        }
    }

    private TaskVriantWithValue getVariantByValue(String value) {
        int i = 0;
        for (TaskVariant tv : variants) {
            for (TaskVariantValue tvv : tv.getVariants().values()) {
                i++;
                if (tvv.getId().equals(value)) {
                    return new TaskVriantWithValue(tv, tvv);
                }
            }
        }
        throw new RuntimeException(value + " not found in " + variants.size() + " variants with total of " + i + " values");
    }

}
