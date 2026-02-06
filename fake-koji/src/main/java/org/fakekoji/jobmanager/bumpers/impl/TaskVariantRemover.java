package org.fakekoji.jobmanager.bumpers.impl;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.BuildDirUpdater;
import org.fakekoji.jobmanager.bumpers.JobModifier;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.util.Map;
import java.util.stream.Collectors;

public class TaskVariantRemover extends JobModifier implements BuildDirUpdater.ArchiveTransformer {

    private final TaskVariant taskVariant;

    public TaskVariantRemover(final AccessibleSettings settings, final TaskVariant taskVariant) {
        super(settings);
        this.taskVariant = taskVariant;
    }

    @Override
    protected boolean shouldPass(BuildJob job) {
        return taskVariant.getType().equals(Task.Type.BUILD);
    }

    @Override
    protected boolean shouldPass(TestJob job) {
        return true;
    }

    private Map<TaskVariant, TaskVariantValue> getVariants(
            final Map<TaskVariant, TaskVariantValue> variants,
            final Task.Type type
    ) {
        if (!taskVariant.getType().equals(type)) {
            return variants;
        }
        return variants.entrySet().stream()
                .filter(e -> !e.getKey().equals(taskVariant))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<TaskVariant, TaskVariantValue> getBuildVariants(final Map<TaskVariant, TaskVariantValue> variants) {
        return getVariants(variants, Task.Type.BUILD);
    }

    private Map<TaskVariant, TaskVariantValue> getTestVariants(final Map<TaskVariant, TaskVariantValue> variants) {
        return getVariants(variants, Task.Type.TEST);
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
                getBuildVariants(job.getVariants()),
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
                getTestVariants(job.getVariants()),
                job.getBuildPlatform(),
                job.getBuildPlatformProvider(),
                job.getBuildTask(),
                getBuildVariants(job.getBuildVariants()),
                job.getProjectSubpackageDenylist(),
                job.getProjectSubpackageAllowlist(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    @Override
    public OToolArchive transform(OToolArchive archive) {
        return new OToolArchive(
                archive,
                archive.getBuildVariants().stream()
                        .filter(tuple -> !tuple.x.equals(taskVariant.getId()))
                        .collect(Collectors.toList())
        );
    }
}
