package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
            String platformProvider,
            String projectName,
            Project.ProjectType projectType,
            Product product,
            JDKVersion jdkVersion,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            Platform buildPlatform,
            Map<TaskVariant, TaskVariantValue> buildVariants,
            List<String> projectSubpackageBlacklist,
            List<String> projectSubpackageWhitelist,
            File scriptsRoot,
            List<OToolVariable> projectVariables
    ) {
        super(platformProvider, projectName, product, jdkVersion, buildProviders, task, platform, variants, scriptsRoot, projectVariables);
        this.projectType = projectType;
        this.buildPlatform = buildPlatform;
        this.buildVariants = buildVariants;
        this.projectSubpackageBlacklist = projectSubpackageBlacklist;
        this.projectSubpackageWhitelist = projectSubpackageWhitelist;
    }

    public TestJob(
            String platformProvider,
            String projectName,
            Project.ProjectType projectType,
            Product product,
            JDKVersion jdkVersion,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            Platform buildPlatform,
            Map<TaskVariant, TaskVariantValue> buildVariants,
            File scriptsRoot,
            List<OToolVariable> projectVariables
    ) {
        this(
                platformProvider,
                projectName,
                projectType,
                product,
                jdkVersion,
                buildProviders,
                task,
                platform,
                variants,
                buildPlatform,
                buildVariants,
                Collections.emptyList(),
                Collections.emptyList(),
                scriptsRoot,
                projectVariables
        );

    }

    public TestJob(
            String testPlatformProvider,
            BuildJob buildJob,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            List<OToolVariable> projectVariables
    ) {
        this(
                testPlatformProvider,
                buildJob.getProjectName(),
                Project.ProjectType.JDK_PROJECT,
                buildJob.getProduct(),
                buildJob.getJdkVersion(),
                buildJob.getBuildProviders(),
                task,
                platform,
                variants,
                buildJob.getPlatform(),
                buildJob.getVariants(),
                buildJob.getScriptsRoot(),
                projectVariables
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
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(loadTemplate(TASK_JOB_TEMPLATE), this)
                .buildBuildProvidersTemplate(getBuildProviders())
                .buildFakeKojiXmlRpcApiTemplate(
                        getProjectName(),
                        getBuildVariants(),
                        fillBuildPlatform(getBuildPlatform(), getTask().getFileRequirements()),
                        true
                )
                .buildScriptTemplate(
                        getTask(),
                        getPlatformProvider(),
                        getPlatform(),
                        getScriptsRoot(),
                        getExportedVariables())
                .buildTriggerTemplate(getTask().getScmPollSchedule())
                .buildPostBuildTasks(getTask().getXmlTemplate())
                .prettyPrint();
    }

    private String generateKojiTemplate() throws IOException {
        final List<String> subpackageBlacklist;
        final List<String> subpackageWhitelist;
        if (getTask().getFileRequirements().getBinary().equals(Task.BinaryRequirements.BINARIES)) {
            //this is workaround
            //only one task is known to require binaries, and that requires them all
            //if ever listing will need filtering also on BINARIES
            //BINARIES and BINARIES_ALL mayb need to be introduced
            subpackageBlacklist = Arrays.asList();
            subpackageWhitelist = Arrays.asList(".*");
        } else {
            subpackageBlacklist = Stream.of(
                    projectSubpackageBlacklist,
                    getTask().getRpmLimitation().getBlacklist(),
                    getBuildVariants()
                            .values()
                            .stream()
                            .map(TaskVariantValue::getSubpackageBlacklist)
                            .map(list -> list.orElse(Collections.emptyList()))
                            .flatMap(List::stream)
                            .collect(Collectors.toList())
            )
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());

            subpackageWhitelist = Stream.of(
                    projectSubpackageWhitelist,
                    getTask().getRpmLimitation().getWhitelist(),
                    getBuildVariants()
                            .values()
                            .stream()
                            .map(TaskVariantValue::getSubpackageWhitelist)
                            .map(list -> list.orElse(Collections.emptyList()))
                            .flatMap(List::stream)
                            .collect(Collectors.toList())
            )
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());
        }

        return XML_DECLARATION + new JenkinsJobTemplateBuilder(loadTemplate(TASK_JOB_TEMPLATE), this)
                .buildBuildProvidersTemplate(getBuildProviders())
                .buildKojiXmlRpcApiTemplate(
                        getProduct().getPackageName(),
                        getBuildPlatform(),
                        getTask().getFileRequirements(),
                        subpackageBlacklist,
                        subpackageWhitelist
                )
                .buildScriptTemplate(
                        getTask(),
                        getPlatformProvider(),
                        getPlatform(),
                        getScriptsRoot(),
                        getExportedVariables())
                .buildTriggerTemplate(getTask().getScmPollSchedule())
                .buildPostBuildTasks(getTask().getXmlTemplate())
                .prettyPrint();
    }

    @Override
    List<OToolVariable> getExportedVariables() {

        final String releaseSuffix = buildVariants.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getValue().getId())
                .collect(Collectors.joining(".")) + '.' + getBuildPlatform().getId();

        final List<OToolVariable> buildVariables = projectType == Project.ProjectType.JDK_PROJECT ?
                Arrays.asList(
                        new OToolVariable(RELEASE_SUFFIX_VAR, releaseSuffix)
                ) : Collections.emptyList();

        final List<OToolVariable> exportedVariables = super.getExportedVariables();
        final List<OToolVariable> variantVariables = OToolVariable.createDefault(buildVariants);
        return Stream.of(
                exportedVariables,
                variantVariables,
                buildVariables
        ).flatMap(List::stream).collect(Collectors.toList());
    }

    public Platform getBuildPlatform() {
        return buildPlatform;
    }

    public Map<TaskVariant, TaskVariantValue> getBuildVariants() {
        return buildVariants;
    }

    @Override
    public String getName() {
        return Job.sanitizeNames(String.join(
                Job.DELIMITER,
                Arrays.asList(
                        getTask().getId(),
                        getProduct().getJdk(),
                        getProjectName(),
                        buildPlatform.getId(),
                        buildVariants.entrySet().stream()
                                .sorted(Comparator.comparing(Map.Entry::getKey))
                                .map(entry -> entry.getValue().getId())
                                .collect(Collectors.joining(Job.DELIMITER)),
                        getRunPlatform(),
                        getVariants().entrySet().stream()
                                .sorted(Comparator.comparing(Map.Entry::getKey))
                                .map(entry -> entry.getValue().getId())
                                .collect(Collectors.joining(Job.DELIMITER))
                )
        ));
    }

    @Override
    public String getShortName() {
        String fullName = getName();
        if (fullName.length() <= MAX_JOBNAME_LENGTH) {
            return fullName;
        } else {
            //this is not same as in etName
            //something is missing and something is shortened
            //TODO extract all but this header creation
            //in test, we should care only about length
            //will be fun to set up project
            String header = Job.sanitizeNames(String.join(
                    Job.DELIMITER,
                    Arrays.asList(
                            getTask().getId(),
                            getProjectName(),
                            buildVariants.entrySet().stream()
                                    .sorted(Comparator.comparing(Map.Entry::getKey))
                                    .map(entry -> Job.firstLetter(entry.getValue().getId()))
                                    .collect(Collectors.joining("")),
                            getRunPlatform(),
                            getVariants().entrySet().stream()
                                    .sorted(Comparator.comparing(Map.Entry::getKey))
                                    .map(entry -> Job.firstLetter(entry.getValue().getId()))
                                    .collect(Collectors.joining(""))
                    )
            ));
            if (header.length() >= Job.MAX_JOBNAME_LENGTH - DELIMITER.length()) {
                return Job.truncatedSha(fullName, Job.MAX_JOBNAME_LENGTH);
            }
            String tail = Job.truncatedSha(fullName, Job.MAX_JOBNAME_LENGTH - header.length() - DELIMITER.length());
            return header + Job.DELIMITER + tail;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestJob)) return false;
        if (!super.equals(o)) return false;
        TestJob testJob = (TestJob) o;
        return projectType == testJob.projectType &&
                Objects.equals(buildPlatform, testJob.buildPlatform) &&
                Objects.equals(buildVariants, testJob.buildVariants) &&
                Objects.equals(projectSubpackageBlacklist, testJob.projectSubpackageBlacklist) &&
                Objects.equals(projectSubpackageWhitelist, testJob.projectSubpackageWhitelist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                projectType,
                buildPlatform,
                buildVariants,
                projectSubpackageBlacklist,
                projectSubpackageWhitelist
        );
    }
}
