package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Product;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.model.TaskVariant;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JDK_VERSION_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.TASK_JOB_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.OJDK_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PACKAGE_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.RELEASE_SUFFIX_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_DECLARATION;

public class BuildJob extends TaskJob {

    public BuildJob(
            String projectName,
            Product product,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            File scriptsRoot
    ) {
        super(projectName, product, buildProviders, task, platform, variants, scriptsRoot);
    }

    @Override
    public String generateTemplate() throws IOException {
        final Product product = getProduct();
        final Map<String, String> variables = new HashMap<String, String>(){{
            put(JDK_VERSION_VAR, product.getVersion());
            put(OJDK_VAR, 'o' + product.getId());
            put(PACKAGE_NAME_VAR, product.getPackageName());
            put(PROJECT_NAME_VAR, getProjectName());
            putAll(getVariants().entrySet()
                    .stream()
                    .collect(Collectors.toMap(key -> key.getKey().getId(), value -> value.getValue().getId())));
            put(RELEASE_SUFFIX_VAR, getVariants().entrySet()
                    .stream()
                    .filter(e -> e.getKey().getType() == Task.Type.BUILD)
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(e -> e.getValue().getId())
                    .collect(Collectors.joining(".")) + '.' + getPlatform().assembleString());
        }};

        // TODO: do this better
        final Map<TaskVariant, TaskVariantValue> variants = new HashMap<TaskVariant, TaskVariantValue>() {{
            putAll(getVariants());
            put(
                    new TaskVariant("buildPlatform", "", Task.Type.BUILD, "", 0, Collections.emptyMap()),
                    new TaskVariantValue(getPlatform().assembleString(), "")
            );
        }};

        return XML_DECLARATION + new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(TASK_JOB_TEMPLATE), this)
                .buildBuildProvidersTemplate(getBuildProviders())
                .buildFakeKojiXmlRpcApiTemplate(
                        getProjectName(),
                        variants,
                        JenkinsJobTemplateBuilder.fillBuildPlatform(getPlatform(), getTask().getFileRequirements()),
                        false
                )
                .buildTriggerTemplate(getTask().getScmPollSchedule())
                .buildScriptTemplate(getTask(), getPlatform(), getScriptsRoot(), variables)
                .buildPostBuildTasks(getTask().getXmlTemplate())
                .prettyPrint();
    }

    @Override
    public String getName() {
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

    @Override
    public String getShortName() {
        return null;
    }
}
