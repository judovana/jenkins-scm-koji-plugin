package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariantValue;

import java.util.ArrayList;
import java.util.List;

class TestSpec implements Spec {
    private final Platform platform;
    private final Platform.Provider provider;
    private final Task task;
    private final List<String> variants;

    //fixme enable compress, so providers and/or platforms and/or variants can disapear

    public TestSpec(Platform platform, Platform.Provider provider, Task task) {
        this.platform = platform;
        this.provider = provider;
        this.task = task;
        this.variants = new ArrayList<>();
    }


    public void addVariant(TaskVariantValue v) {
        variants.add(v.getId());
    }

    @Override
    public String toString() {
        return TaskJob.getPlatformAndProviderString(platform, provider.getId()) +
                "-" + task.getId() + getVariants();
    }

    private String getVariants() {
        if (variants.size() > 0) {
            return "-" + String.join(".", variants);
        } else {
            return "";
        }
    }
}
