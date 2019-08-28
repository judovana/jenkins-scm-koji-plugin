package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.Product;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.PULL_JOB_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_DECLARATION;

public class PullJob implements Job {

    public static final String PULL = "pull";

    private final String projectName;
    private final Product product;
    private final Set<String> buildVariants;
    private final File repositoriesRoot;

    public PullJob(
            String projectName,
            Product product,
            Set<String> buildVariants,
            File repositoriesRoot
    ) {
        this.projectName = projectName;
        this.product = product;
        this.buildVariants = buildVariants;
        this.repositoriesRoot = repositoriesRoot;
    }

    @Override
    public String generateTemplate() throws IOException {
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder.loadTemplate(PULL_JOB_TEMPLATE))
                .buildPullScriptTemplate(projectName, product, buildVariants, repositoriesRoot.getAbsolutePath())
                .prettyPrint();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PullJob)) return false;
        PullJob pullJob = (PullJob) o;
        return Objects.equals(projectName, pullJob.projectName) &&
                Objects.equals(product, pullJob.product) &&
                Objects.equals(buildVariants, pullJob.buildVariants) &&
                Objects.equals(repositoriesRoot, pullJob.repositoriesRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, product, buildVariants, repositoriesRoot);
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
