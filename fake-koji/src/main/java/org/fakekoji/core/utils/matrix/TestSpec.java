package org.fakekoji.core.utils.matrix;

import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;

class TestSpec extends Spec {
    private final Task task;


    //fixme enable compress, so providers and/or platforms and/or variants can disapear

    public TestSpec(Platform platform, Platform.Provider provider, Task task) {
        super(platform, provider);
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    @Override
    public String toString() {
        return TaskJob.getPlatformAndProviderString(platform, provider.getId()) +
                "-" + task.getId() + getVariantsString();
    }

}
