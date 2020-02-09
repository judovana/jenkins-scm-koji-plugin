package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;

class BuildSpec extends Spec {

    private final Project project;


    public BuildSpec(Platform platform, Platform.Provider provider, Project project, BuildEqualityFilter viewFilter) {
        super(platform, provider, viewFilter);
        this.project = project;

    }

    private BuildEqualityFilter getViewFilter() {
        return (BuildEqualityFilter) viewFilter;
    }

    @Override
    public String toString() {
        String prj = project.getProduct().getJdk() + "-" + project.getId();
        if (!getViewFilter().project){
            prj = "?";
        }
        return getPlatformString()+
                "-" + prj +
                getVariantsString();

    }

    public boolean matchProject(String id){
        return !getViewFilter().project || project.getId().equals(id);
    }

}
