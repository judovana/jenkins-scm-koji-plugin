package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;

import java.util.List;

class BuildSpec extends Spec {

    private final Project project;


    public BuildSpec(
            final Platform platform,
            final Platform.Provider provider,
            final Project project,
            final List<String> variants,
            final BuildEqualityFilter viewFilter) {
        super(platform, provider, viewFilter, variants);
        this.project = project;

    }

    private BuildEqualityFilter getViewFilter() {
        return (BuildEqualityFilter) viewFilter;
    }

    @Override
    public String toString() {
        String jdk= project.getProduct().getJdk();
        if (!getViewFilter().jdk){
            jdk = "?";
        }

        String prj = project.getId();
        if (!getViewFilter().project){
            prj = "?";
        }
        return getPlatformString()+
                "-" + jdk + "-" + prj +
                getVariantsString();

    }

    public boolean matchProject(String id){
        return !getViewFilter().project || project.getId().equals(id);
    }

    public boolean matchJdk(String jdk){
        return !getViewFilter().jdk || project.getProduct().getJdk().equals(jdk);
    }

}
