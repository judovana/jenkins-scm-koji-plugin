package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;

import java.util.List;

class BuildSpec extends Spec {

    private final String projectName;
    private final Product product;

    public BuildSpec(
            final Platform platform,
            final Platform.Provider provider,
            final Project project,
            final List<String> variants,
            final BuildEqualityFilter viewFilter) {
        this(platform, provider, project.getId(), project.getProduct(), variants, viewFilter);
    }

    public BuildSpec(
            final Platform platform,
            final Platform.Provider provider,
            final String projectName,
            final Product product,
            final List<String> variants,
            final BuildEqualityFilter viewFilter) {
        super(platform, provider, viewFilter, variants);
        this.projectName = projectName;
        this.product = product;
    }

    private BuildEqualityFilter getViewFilter() {
        return (BuildEqualityFilter) viewFilter;
    }

    @Override
    public String toString() {
        String jdk= product.getJdk();
        if (!getViewFilter().jdk){
            jdk = "?";
        }

        String prj = projectName;
        if (!getViewFilter().project){
            prj = "?";
        }
        return getPlatformString()+
                "-" + jdk + "-" + prj +
                getVariantsString();

    }
}
