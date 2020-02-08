package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;

class BuildSpec extends Spec {

    private final Project project;


    public BuildSpec(Platform platform, Platform.Provider provider, Project project, EqualityFilter viewFilter) {
        super(platform, provider, viewFilter);
        this.project = project;

    }

    public Project getProject() {
        return project;
    }

    @Override
    public String toString() {
        String prj = project.getProduct().getJdk() + "-" + project.getId();
        if (!viewFilter.suiteOrProject){
            prj = "?";
        }
        return getPlatformString()+
                "-" + prj +
                getVariantsString();

    }

}
