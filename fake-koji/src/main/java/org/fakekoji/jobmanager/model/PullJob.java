package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.JDKVersion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JDK_VERSION_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.PULL_JOB_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.NO_CHANGE_RETURN_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PACKAGE_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_NAME_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_PATH_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_DECLARATION;

public class PullJob extends Job {

    public static final String PULL = "pull";

    private final String projectName;
    private final Product product;
    private final JDKVersion jdkVersion;
    private final File repositoriesRoot;
    private final File scriptsRoot;

    public PullJob(
            String projectName,
            Product product,
            JDKVersion jdkVersion,
            File repositoriesRoot,
            File scriptsRoot
    ) {
        this.projectName = projectName;
        this.product = product;
        this.jdkVersion = jdkVersion;
        this.repositoriesRoot = repositoriesRoot;
        this.scriptsRoot = scriptsRoot;
    }

    @Override
    public String generateTemplate() throws IOException {
        final List<JenkinsJobTemplateBuilder.Variable> exportedVariables = new ArrayList<JenkinsJobTemplateBuilder.Variable>() {{
            add(new JenkinsJobTemplateBuilder.Variable(JDK_VERSION_VAR, jdkVersion.getVersion()));
            add(new JenkinsJobTemplateBuilder.Variable(PACKAGE_NAME_VAR, product.getPackageName()));
            add(new JenkinsJobTemplateBuilder.Variable(PROJECT_NAME_VAR, projectName));
            add(new JenkinsJobTemplateBuilder.Variable(PROJECT_PATH_VAR, Paths.get(repositoriesRoot.getAbsolutePath(), projectName).toString()));
            add(new JenkinsJobTemplateBuilder.Variable(false, true, "any negative is enforcing pull even without changes detected", NO_CHANGE_RETURN_VAR, "-1", true));
        }};
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(PULL_JOB_TEMPLATE), this)
                .buildPullScriptTemplate(exportedVariables, scriptsRoot)
                .buildTriggerTemplate("1 1 1 12 *") // run once a year
                .prettyPrint();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PullJob)) return false;
        PullJob pullJob = (PullJob) o;
        return Objects.equals(projectName, pullJob.projectName) &&
                Objects.equals(product, pullJob.product) &&
                Objects.equals(jdkVersion, pullJob.jdkVersion) &&
                Objects.equals(repositoriesRoot, pullJob.repositoriesRoot);
    }

    @Override
    public String getName() {
        return Job.sanitizeNames(String.join(
                Job.DELIMITER,
                Arrays.asList(
                        PULL,
                        product.getJdk(),
                        projectName
                )
        ));
    }

    @Override
    public String getShortName() {
        String fullName = getName();
        if (fullName.length() < MAX_JOBNAME_LENGTH) {
            return fullName;
        } else {
            throw new RuntimeException("pull job name can not be shortened!");
        }
    }
}
