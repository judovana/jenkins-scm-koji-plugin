package org.fakekoji.jobmanager.bumpers;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.BuildDirUpdater;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaskVariantAdder extends JobModifier implements BuildDirUpdater.ArchiveTransformer {

    private final TaskVariant taskVariant;

    public TaskVariantAdder(final AccessibleSettings settings, final TaskVariant taskVariant) {
        super(settings);
        this.taskVariant = taskVariant;
    }
    
    @Override
    boolean shouldPass(BuildJob job) {
        return taskVariant.getType().equals(Task.Type.BUILD);
    }

    @Override
    boolean shouldPass(TestJob job) {
        return true;
    }
    
    private Map<TaskVariant, TaskVariantValue> getBuildVariants(final Map<TaskVariant, TaskVariantValue> variants) {
        if (taskVariant.getType() == Task.Type.TEST) {
            return variants;
        }
        return Stream.concat(
                variants.entrySet().stream(),
                Stream.of(new AbstractMap.SimpleEntry<>(taskVariant, taskVariant.getVariants().get(taskVariant.getDefaultValue())))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<TaskVariant, TaskVariantValue> getTestVariants(final Map<TaskVariant, TaskVariantValue> variants) {
        if (taskVariant.getType() == Task.Type.BUILD) {
            return variants;
        }
        return Stream.concat(
                variants.entrySet().stream(),
                Stream.of(new AbstractMap.SimpleEntry<>(taskVariant, taskVariant.getVariants().get(taskVariant.getDefaultValue())))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    BuildJob transform(BuildJob job) {
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
    TestJob transform(TestJob job) {
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
                job.getProjectSubpackageBlacklist(),
                job.getProjectSubpackageWhitelist(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    @Override
    public OToolArchive transform(OToolArchive archive) {
        return archive;
    }
}
