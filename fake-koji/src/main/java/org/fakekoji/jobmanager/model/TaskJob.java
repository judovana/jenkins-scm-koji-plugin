package org.fakekoji.jobmanager.model;

import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantCategory;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class TaskJob extends Job {

    private final String projectName;
    private final Product product;
    private final Set<BuildProvider> buildProviders;
    private final Task task;
    private final Platform platform;
    private final Map<TaskVariantCategory, TaskVariant> variants;
    private final File scriptsRoot;

    TaskJob(
            String projectName,
            Product product,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariantCategory,
            TaskVariant> variants,
            File scriptsRoot
    ) {
        this.projectName = projectName;
        this.product = product;
        this.buildProviders = buildProviders;
        this.task = task;
        this.platform = platform;
        this.variants = variants;
        this.scriptsRoot = scriptsRoot;
    }

    public String getProjectName() {
        return projectName;
    }

    public Product getProduct() {
        return product;
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

    public Map<TaskVariantCategory, TaskVariant> getVariants() {
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
        return Objects.equals(projectName, taskJob.projectName) &&
                Objects.equals(product, taskJob.product) &&
                Objects.equals(buildProviders, taskJob.buildProviders) &&
                Objects.equals(task, taskJob.task) &&
                Objects.equals(platform, taskJob.platform) &&
                Objects.equals(variants, taskJob.variants) &&
                Objects.equals(scriptsRoot, taskJob.scriptsRoot);
    }
}
