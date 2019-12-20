package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.Product;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
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
    private final File repositoriesRoot;
    private final File scriptsRoot;

    public PullJob(
            String projectName,
            Product product,
            File repositoriesRoot,
            File scriptsRoot
    ) {
        this.projectName = projectName;
        this.product = product;
        this.repositoriesRoot = repositoriesRoot;
        this.scriptsRoot = scriptsRoot;
    }

    @Override
    public String generateTemplate() throws IOException {
        final Map<String, String> exportedVariables = new HashMap<String, String>() {{
            put(JDK_VERSION_VAR, product.getVersion());
            put(PACKAGE_NAME_VAR, product.getPackageName());
            put(PROJECT_NAME_VAR, projectName);
            put(PROJECT_PATH_VAR, Paths.get(repositoriesRoot.getAbsolutePath(), projectName).toString());
            put(NO_CHANGE_RETURN_VAR, "-1");
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
                Objects.equals(repositoriesRoot, pullJob.repositoriesRoot);
    }

    @Override
    public String getName() {
        return String.join(
                Job.DELIMITER,
                Arrays.asList(
                        PULL,
                        product.getId(),
                        projectName
                )
        );
    }

    @Override
    public String getShortName() {
        return null;
    }
}
