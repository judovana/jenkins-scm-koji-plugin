package org.fakekoji.core.utils.matrix;

import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;

import java.util.Collections;
import java.util.List;

class TestSpec extends Spec {
    private final Task task;


    public TestSpec(
            final Platform platform,
            final Platform.Provider provider,
            final Task task,
            final List<String> variants,
            final TestEqualityFilter viewFilter
    ) {
        super(platform, provider, viewFilter, variants);
        this.task = task;
    }

    public TestSpec(
            final Platform platform,
            final Platform.Provider provider,
            final Task task,
            final TestEqualityFilter viewFilter
    ) {
        super(platform, provider, viewFilter, Collections.emptyList());
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
