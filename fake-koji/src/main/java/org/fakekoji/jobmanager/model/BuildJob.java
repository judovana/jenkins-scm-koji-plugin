package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.BuildProvider;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.model.TaskVariant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
            JDKVersion jdkVersion,
            Set<BuildProvider> buildProviders,
            Task task,
            Platform platform,
            Map<TaskVariant, TaskVariantValue> variants,
            File scriptsRoot
    ) {
        super(projectName, product, jdkVersion, buildProviders, task, platform, variants, scriptsRoot);
    }

    @Override
    public String generateTemplate() throws IOException {
        final List<JenkinsJobTemplateBuilder.Variable> variables = new ArrayList<JenkinsJobTemplateBuilder.Variable>() {{
            add(new JenkinsJobTemplateBuilder.Variable(JDK_VERSION_VAR, getJdkVersion().getVersion()));
            add(new JenkinsJobTemplateBuilder.Variable(OJDK_VAR, 'o' + getProduct().getJdk()));
            add(new JenkinsJobTemplateBuilder.Variable(PACKAGE_NAME_VAR, getProduct().getPackageName()));
            add(new JenkinsJobTemplateBuilder.Variable(PROJECT_NAME_VAR, getProjectName()));
            addAll(JenkinsJobTemplateBuilder.Variable.createDefault(getVariants()));
            add(new JenkinsJobTemplateBuilder.Variable(RELEASE_SUFFIX_VAR, getVariants().entrySet()
                    .stream()
                    .filter(e -> e.getKey().getType() == Task.Type.BUILD)
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(e -> e.getValue().getId())
                    .collect(Collectors.joining(".")) + '.' + getPlatform().assembleString()));
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
        return Job.sanitizeNames(String.join(
                Job.DELIMITER,
                Arrays.asList(
                        getTask().getId(),
                        getProduct().getJdk(),
                        getProjectName(),
                        getPlatform().getId(),
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
        if (fullName.length() < MAX_JOBNAME_LENGTH) {
            return fullName;
        } else {
            //this is not same as in etName
            //something is missing and something is shortened
            //TODO extract all but this header creation
            //in test, we should care only about length
            //will be fun to set up project
            String header = sanitizeNames(String.join(
                    Job.DELIMITER,
                    Arrays.asList(
                            getTask().getId(),
                            getProjectName(),
                            getPlatform().getId(),
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
}
