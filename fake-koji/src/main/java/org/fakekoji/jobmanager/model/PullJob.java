package org.fakekoji.jobmanager.model;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolVariable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.JenkinsTemplate.PULL_JOB_TEMPLATE;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.NO_CHANGE_RETURN_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.PROJECT_PATH_VAR;
import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.XML_DECLARATION;

public class PullJob extends Job {

    public static final String PULL = "pull";

    private final String repoUrl;
    private final File repositoriesRoot;
    private final File scriptsRoot;

    public PullJob(
            String projectName,
            String repoUrl,
            Product product,
            JDKVersion jdkVersion,
            File repositoriesRoot,
            File scriptsRoot,
            List<OToolVariable> projectVariables
    ) {
        super(projectName, projectVariables, product, jdkVersion);
        this.repoUrl = repoUrl;
        this.repositoriesRoot = repositoriesRoot;
        this.scriptsRoot = scriptsRoot;
    }

    @Override
    public String generateTemplate() throws IOException {
        return XML_DECLARATION + new JenkinsJobTemplateBuilder(JenkinsJobTemplateBuilder
                .loadTemplate(PULL_JOB_TEMPLATE), this)
                .buildPullScriptTemplate(getExportedVariables(), scriptsRoot)
                .buildTriggerTemplate("1 1 1 12 *") // run once a year
                .prettyPrint();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PullJob)) return false;
        if (!super.equals(o)) return false;
        PullJob pullJob = (PullJob) o;
        return Objects.equals(repoUrl, pullJob.repoUrl) &&
                Objects.equals(repositoriesRoot, pullJob.repositoriesRoot) &&
                Objects.equals(scriptsRoot, pullJob.scriptsRoot);
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public File getRepositoriesRoot() {
        return repositoriesRoot;
    }

    public File getScriptsRoot() {
        return scriptsRoot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                repoUrl,
                repositoriesRoot,
                scriptsRoot
        );
    }

    @Override
    public String getName() {
        return Job.sanitizeNames(String.join(
                Job.DELIMITER,
                Arrays.asList(
                        PULL,
                        this.getProduct().getJdk(),
                        getProjectName()
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

    @Override
    List<OToolVariable> getExportedVariables() {
        return Stream.concat(
                super.getExportedVariables().stream(),
                Stream.of(
                        new OToolVariable(
                                PROJECT_PATH_VAR,
                                Paths.get(repositoriesRoot.getAbsolutePath(), getProjectName()).toString()
                        ),
                        new OToolVariable(
                                NO_CHANGE_RETURN_VAR,
                                "-1",
                                "any negative is enforcing pull even without changes detected",
                                false,
                                true,
                                true
                        )
                        //not propagaing project specific vars right now
                )
        ).collect(Collectors.toList());
    }
}
