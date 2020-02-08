package org.fakekoji.core.utils.matrix;

import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;

class TestSpec extends Spec {
    private final Task task;


    public TestSpec(Platform platform, Platform.Provider provider, Task task, EqualityFilter viewFilter) {
        super(platform, provider, viewFilter);
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    @Override
    public String toString() {
        String tsk = task.getId();
        if (!viewFilter.suiteOrProject) {
            tsk = "?";
        }
        return getPlatformString()+
                "-" + tsk + getVariantsString();
    }

}
