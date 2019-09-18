package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantCategory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.TASK_JOB_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_DECLARATION;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.fillBuildPlatform;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.loadTemplate;

public class TestJob extends TaskJob {

    private final Platform buildPlatform;
    private final Map<TaskVariantCategory, TaskVariant> buildVariants;

    public TestJob(
            String projectName,
            Product product,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariantCategory, TaskVariant> variants,
            Platform buildPlatform,
            Map<TaskVariantCategory, TaskVariant> buildVariants,
            File scriptsRoot
    ) {
        super(projectName, product, buildProviders, task, platform, variants, scriptsRoot);
        this.buildPlatform = buildPlatform;
        this.buildVariants = buildVariants;
    }

    public TestJob(
            BuildJob buildJob,
            Task task,
            Platform platform,
            Map<TaskVariantCategory, TaskVariant> variants
    ) {
        this(
                buildJob.getProjectName(),
                buildJob.getProduct(),
                buildJob.getBuildProviders(),
                task,
                platform,
                variants,
                buildJob.getPlatform(),
                buildJob.getVariants(),
                buildJob.getScriptsRoot()
        );
    }

    @Override
    public String generateTemplate() throws IOException {
        final Map<TaskVariantCategory, TaskVariant> variants = new HashMap<>();
        variants.putAll(getBuildVariants());
        variants.putAll(getVariants());
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(loadTemplate(TASK_JOB_TEMPLATE))
                .buildBuildProvidersTemplate(getBuildProviders())
                .buildFakeKojiXmlRpcApiTemplate(
                        getProjectName(),
                        getBuildVariants(),
                        fillBuildPlatform(getBuildPlatform(), getTask().getFileRequirements()),
                        true
                )
                .buildScriptTemplate(getTask(), getPlatform(), variants, getScriptsRoot())
                .buildPostBuildTasks(getTask().getXmlTemplate())
                .prettyPrint();
    }

    public Platform getBuildPlatform() {
        return buildPlatform;
    }

    public Map<TaskVariantCategory, TaskVariant> getBuildVariants() {
        return buildVariants;
    }

    @Override
    public String toString() {
        return String.join(
                Job.DELIMITER,
                Arrays.asList(
                        getTask().getId(),
                        getProduct().getId(),
                        getProjectName(),
                        buildPlatform.getString(),
                        buildVariants.entrySet().stream()
                                .sorted(Comparator.comparing(Map.Entry::getKey))
                                .map(entry -> entry.getValue().getId())
                                .collect(Collectors.joining(Job.DELIMITER)),
                        getPlatform().getId(),
                        getVariants().entrySet().stream()
                                .sorted(Comparator.comparing(Map.Entry::getKey))
                                .map(entry -> entry.getValue().getId())
                                .collect(Collectors.joining(Job.DELIMITER))
                )
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestJob)) return false;
        if (!super.equals(o)) return false;
        TestJob testJob = (TestJob) o;
        return Objects.equals(buildPlatform, testJob.buildPlatform) &&
                Objects.equals(buildVariants, testJob.buildVariants);
    }
}
