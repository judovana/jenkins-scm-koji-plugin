package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantCategory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.TASK_JOB_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_DECLARATION;

public class BuildJob extends TaskJob {

    public BuildJob(
            String projectName,
            Product product,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariantCategory, TaskVariant> variants
    ) {
        super(projectName, product, buildProviders, task, platform, variants);
    }

    @Override
    public String generateTemplate() throws IOException {
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(TASK_JOB_TEMPLATE))
                .buildBuildProvidersTemplate(getBuildProviders())
                .buildFakeKojiXmlRpcApiTemplate(
                        getProjectName(),
                        getVariants(),
                        JenkinsJobTemplateBuilder.fillBuildPlatform(getPlatform(), getTask().getFileRequirements()),
                        false
                )
                .buildScriptTemplate(getTask(), getPlatform(), getVariants())
                .buildPostBuildTasks(getTask().getXmlTemplate())
                .prettyPrint();
    }

    @Override
    public String toString() {
        return String.join(
                Job.DELIMITER,
                Arrays.asList(
                        getTask().getId(),
                        getProduct().getId(),
                        getProjectName(),
                        getPlatform().getId(),
                        getVariants().entrySet().stream()
                                .sorted(Comparator.comparing(Map.Entry::getKey))
                                .map(entry -> entry.getValue().getId())
                                .collect(Collectors.joining(Job.DELIMITER))
                )
        );
    }
}
