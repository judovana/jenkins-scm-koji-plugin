package org.fakekoji.jobmanager.model;

import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TaskJob extends Job {

    private final String platformProvider;
    private final Set<BuildProvider> buildProviders;
    private final Task task;
    private final Platform platform;
    private final Map<TaskVariant, TaskVariantValue> variants;
    private final File scriptsRoot;

    TaskJob(
            String platformProvider,
            String projectName,
            Product product,
            JDKVersion jdkVersion,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            File scriptsRoot,
            List<OToolVariable> projectVariables
    ) {
        super(projectName, projectVariables, product, jdkVersion);
        this.platformProvider = platformProvider;
        this.buildProviders = buildProviders;
        this.task = task;
        this.platform = platform;
        this.variants = variants;
        this.scriptsRoot = scriptsRoot;
    }

    public String getPlatformProvider() {
        return platformProvider;
    }

    public Set<BuildProvider> getBuildProviders() {
        return buildProviders;
    }

    public Task getTask() {
        return task;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Map<TaskVariant, TaskVariantValue> getVariants() {
        return variants;
    }

    public File getScriptsRoot() {
        return scriptsRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskJob)) return false;
        TaskJob taskJob = (TaskJob) o;
        return Objects.equals(platformProvider, taskJob.platformProvider) &&
                Objects.equals(buildProviders, taskJob.buildProviders) &&
                Objects.equals(task, taskJob.task) &&
                Objects.equals(platform, taskJob.platform) &&
                Objects.equals(variants, taskJob.variants) &&
                Objects.equals(scriptsRoot, taskJob.scriptsRoot) &&
                Objects.equals(getProjectVariables(), taskJob.getProjectVariables()) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                platformProvider,
                buildProviders,
                task,
                platform,
                variants,
                scriptsRoot,
                getProjectVariables()
        );
    }

    @Override
    List<OToolVariable> getExportedVariables() {
        final List<OToolVariable> defaultVariables = new ArrayList<>(super.getExportedVariables());
        getPlatform().addZstreamVar(defaultVariables);
        getPlatform().addYstreamVar(defaultVariables);
        defaultVariables.addAll(getProjectVariables());
        final List<OToolVariable> variantVariables = OToolVariable.createDefault(variants);
        return Stream.of(
                defaultVariables,
                variantVariables,
                getTask().getVariables(),
                getPlatform().getVariables()
        ).flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public String getRunPlatform() {
        return getPlatformAndProviderString(platform, platformProvider);
    }

    public static String getPlatformAndProviderString(Platform pl, String pr) {
        if (pr == null || pr.isEmpty()) {
            return pl.getId();
        } else {
            return pl.getId() + '.' + pr;
        }
    }
}
