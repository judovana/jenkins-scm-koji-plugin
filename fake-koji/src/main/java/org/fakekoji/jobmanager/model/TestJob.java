package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.TASK_JOB_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.RELEASE_SUFFIX_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_DECLARATION;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.fillBuildPlatform;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.loadTemplate;

public class TestJob extends TaskJob {

    private final Project.ProjectType projectType;
    private final Platform buildPlatform;
    private final Map<TaskVariant, TaskVariantValue> buildVariants;
    private final List<String> projectSubpackageBlacklist;
    private final List<String> projectSubpackageWhitelist;

    public TestJob(
            String projectName,
            Project.ProjectType projectType,
            Product product,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            Platform buildPlatform,
            Map<TaskVariant, TaskVariantValue> buildVariants,
            List<String> projectSubpackageBlacklist,
            List<String> projectSubpackageWhitelist,
            File scriptsRoot
    ) {
        super(projectName, product, buildProviders, task, platform, variants, scriptsRoot);
        this.projectType = projectType;
        this.buildPlatform = buildPlatform;
        this.buildVariants = buildVariants;
        this.projectSubpackageBlacklist = projectSubpackageBlacklist;
        this.projectSubpackageWhitelist = projectSubpackageWhitelist;
    }

    public TestJob(
            String projectName,
            Project.ProjectType projectType,
            Product product,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            Platform buildPlatform,
            Map<TaskVariant, TaskVariantValue> buildVariants,
            File scriptsRoot
    ) {
        this(
                projectName,
                projectType,
                product,
                buildProviders,
                task,
                platform,
                variants,
                buildPlatform,
                buildVariants,
                Collections.emptyList(),
                Collections.emptyList(),
                scriptsRoot
        );

    }

    public TestJob(
            BuildJob buildJob,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants
    ) {
        this(
                buildJob.getProjectName(),
                Project.ProjectType.JDK_PROJECT,
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
        switch (projectType) {
            case JDK_PROJECT:
                return generateOToolTemplate();
            case JDK_TEST_PROJECT:
                return generateKojiTemplate();
        }
        return "";
    }

    private String generateOToolTemplate() throws IOException {
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(loadTemplate(TASK_JOB_TEMPLATE))
                .buildBuildProvidersTemplate(getBuildProviders())
                .buildFakeKojiXmlRpcApiTemplate(
                        getProjectName(),
                        getBuildVariants(),
                        fillBuildPlatform(getBuildPlatform(), getTask().getFileRequirements()),
                        true
                )
                .buildScriptTemplate(
                        getTask(),
                        getPlatform(),
                        getScriptsRoot(),
                        getExportedVariables())
                .buildTriggerTemplate(getTask().getScmPollSchedule())
                .buildPostBuildTasks(getTask().getXmlTemplate())
                .prettyPrint();
    }

    private String generateKojiTemplate() throws IOException {

        return XML_DECLARATION + new JenkinsJobTemplateBuilder(loadTemplate(TASK_JOB_TEMPLATE))
                .buildBuildProvidersTemplate(getBuildProviders())
                .buildKojiXmlRpcApiTemplate(
                        getProduct().getPackageName(),
                        getBuildPlatform().getArchitecture(),
                        getBuildPlatform().getTags(),
                        Stream.of(projectSubpackageBlacklist, getTask().getRpmLimitation().getBlacklist())
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()),
                        Stream.of(projectSubpackageWhitelist, getTask().getRpmLimitation().getWhitelist())
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                )
                .buildScriptTemplate(
                        getTask(),
                        getPlatform(),
                        getScriptsRoot(),
                        getExportedVariables())
                .buildTriggerTemplate(getTask().getScmPollSchedule())
                .buildPostBuildTasks(getTask().getXmlTemplate())
                .prettyPrint();
    }

    @Override
    Map<String, String> getExportedVariables() {
        final Map<String, String> exportedVariables = super.getExportedVariables();
        exportedVariables.putAll(buildVariants.entrySet()
                .stream()
                .collect(Collectors.toMap(key -> key.getKey().getId(), value -> value.getValue().getId())));
        if (projectType == Project.ProjectType.JDK_PROJECT) {
            exportedVariables.put(RELEASE_SUFFIX_VAR, buildVariants.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(entry -> entry.getValue().getId())
                    .collect(Collectors.joining(".")) + '.' + getBuildPlatform().assembleString());
        }
        return exportedVariables;
    }

    public Platform getBuildPlatform() {
        return buildPlatform;
    }

    public Map<TaskVariant, TaskVariantValue> getBuildVariants() {
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
                        buildPlatform.assembleString(),
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
