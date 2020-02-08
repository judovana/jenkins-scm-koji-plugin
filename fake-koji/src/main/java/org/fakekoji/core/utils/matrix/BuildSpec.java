package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.model.Platform;

class BuildSpec extends Spec {

    private final Project project;

    //fixme enable compress, so providers and/or platforms and/or variants can disapear

    public BuildSpec(Platform platform, Platform.Provider provider, Project project) {
        super(platform, provider);
        this.project = project;

    }

    public Project getProject() {
        return project;
    }

    @Override
    public String toString() {
        return TaskJob.getPlatformAndProviderString(platform, provider.getId()) +
                "-" + project.getProduct().getJdk() + "-" + project.getId() +
                "-" + String.join(".", variants);
    }

}
