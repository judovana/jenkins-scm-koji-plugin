package org.fakekoji.jobmanager.bumpers.impl;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.bumpers.JobModifier;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.TestJob;

import java.util.regex.Pattern;

public class ProviderBumper extends JobModifier {

    private final String from;
    private final String to;
    private final Pattern filter;

    public ProviderBumper(
            final AccessibleSettings settings,
            final String from,
            final String to,
            final Pattern filter
    ) {
        super(settings);
        this.from = from;
        this.to = to;
        this.filter = filter;
    }

    @Override
    protected boolean shouldPass(BuildJob job) {
        return job.getPlatformProvider().equals(from) && matches(job.getName());
    }

    @Override
    protected boolean shouldPass(TestJob job) {
        return job.getPlatformProvider().equals(from) && matches(job.getName());
    }

    private boolean matches(String name) {
        return  filter.matcher(name).matches();
    }

    @Override
    protected BuildJob transform(BuildJob job) {
        return new BuildJob(
                to,
                job.getProjectName(),
                job.getProduct(),
                job.getJdkVersion(),
                job.getBuildProviders(),
                job.getTask(),
                job.getPlatform(),
                job.getVariants(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    @Override
    protected  TestJob transform(TestJob job) {
        return new TestJob(
                to,
                job.getProjectName(),
                job.getProjectType(),
                job.getProduct(),
                job.getJdkVersion(),
                job.getBuildProviders(),
                job.getTask(),
                job.getPlatform(),
                job.getVariants(),
                job.getBuildPlatform(),
                job.getBuildPlatformProvider(),
                job.getBuildTask(),
                job.getBuildVariants(),
                job.getProjectSubpackageDenylist(),
                job.getProjectSubpackageAllowlist(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

}
