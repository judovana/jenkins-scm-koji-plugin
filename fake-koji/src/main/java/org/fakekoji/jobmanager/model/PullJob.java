package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.Product;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.PULL_JOB_TEMPLATE;
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
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(PULL_JOB_TEMPLATE))
                .buildPullScriptTemplate(projectName, product, repositoriesRoot.getAbsolutePath(), scriptsRoot)
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
    public String toString() {
        return String.join(
                Job.DELIMITER,
                Arrays.asList(
                        PULL,
                        product.getId(),
                        projectName
                )
        );
    }
}
