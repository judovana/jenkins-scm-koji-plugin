package org.fakekoji.core.utils.matrix;

import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;

class TestSpec extends Spec {
    private final Task task;


    public TestSpec(Platform platform, Platform.Provider provider, Task task, TestEqualityFilter viewFilter) {
        super(platform, provider, viewFilter);
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    private TestEqualityFilter getViewFilter() {
        return (TestEqualityFilter) viewFilter;
    }

    @Override
    public String toString() {
        String tsk = task.getId();
        if (!getViewFilter().suite) {
            tsk = "?";
        }
        return getPlatformString()+
                "-" + tsk + getVariantsString();
    }

    public boolean matchSuite(String id){
        return !getViewFilter().suite || task.getId().equals(id);
    }

}
