package org.fakekoji.jobmanager.model;

import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JDK_VERSION_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.OJDK_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PACKAGE_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_NAME_VAR;

public abstract class TaskJob extends Job {

    private final String platformProvider;
    private final String projectName;
    private final Product product;
    private final JDKVersion jdkVersion;
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
            File scriptsRoot
    ) {
        this.platformProvider = platformProvider;
        this.projectName = projectName;
        this.product = product;
        this.jdkVersion = jdkVersion;
        this.buildProviders = buildProviders;
        this.task = task;
        this.platform = platform;
        this.variants = variants;
        this.scriptsRoot = scriptsRoot;
    }

    public String getPlatformProvider() {
        return platformProvider;
    }

    public String getProjectName() {
        return projectName;
    }

    public Product getProduct() {
        return product;
    }

    public JDKVersion getJdkVersion() {
        return jdkVersion;
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
                Objects.equals(projectName, taskJob.projectName) &&
                Objects.equals(product, taskJob.product) &&
                Objects.equals(jdkVersion, taskJob.jdkVersion) &&
                Objects.equals(buildProviders, taskJob.buildProviders) &&
                Objects.equals(task, taskJob.task) &&
                Objects.equals(platform, taskJob.platform) &&
                Objects.equals(variants, taskJob.variants) &&
                Objects.equals(scriptsRoot, taskJob.scriptsRoot);
    }

    List<OToolVariable> getExportedVariables() {
        final List<OToolVariable> defaultVariables = Arrays.asList(
                new OToolVariable(JDK_VERSION_VAR, jdkVersion.getVersion()),
                new OToolVariable(OJDK_VAR, jdkVersion.getId()),
                new OToolVariable(PACKAGE_NAME_VAR, product.getPackageName()),
                new OToolVariable(PROJECT_NAME_VAR, projectName)
        );
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
