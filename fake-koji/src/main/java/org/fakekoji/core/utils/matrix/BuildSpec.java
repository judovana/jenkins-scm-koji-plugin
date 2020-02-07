package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.model.Platform;
import org.fakekoji.model.TaskVariantValue;

import java.util.ArrayList;
import java.util.List;

class BuildSpec implements Spec {

    private final Platform platform;
    private final Platform.Provider provider;
    private final Project project;
    private final List<String> variants;

    //fixme enable compress, so providers and/or platforms and/or variants can disapear

    public BuildSpec(Platform platform, Platform.Provider provider, Project project) {
        this.platform = platform;
        this.provider = provider;
        this.project = project;
        this.variants = new ArrayList<>();
    }

    public void addVariant(TaskVariantValue v) {
        variants.add(v.getId());
    }

    @Override
    public String toString() {
        return TaskJob.getPlatformAndProviderString(platform, provider.getId()) +
                "-" + project.getProduct().getJdk() + "-" + project.getId() +
                "-" + String.join(".", variants);
    }
}
