package org.fakekoji.jobmanager;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.Platform;

public class PlatformBumper extends JobModifier {

    private final Platform from;
    private final Platform to;

    public PlatformBumper(
            final AccessibleSettings settings,
            final Platform from,
            final Platform to
    ) {
        super(settings);
        this.from = from;
        this.to = to;
    }

    @Override
    boolean shouldPass(BuildJob job) {
        return isFromPlatform(job.getPlatform());
    }

    @Override
    boolean shouldPass(TestJob job) {
        return isFromPlatform(job.getPlatform()) || isFromBuildPlatform(job.getBuildPlatform());
    }

    @Override
    BuildJob transform(BuildJob job) {
        return new BuildJob(
                job.getPlatformProvider(),
                job.getProjectName(),
                job.getProduct(),
                job.getJdkVersion(),
                job.getBuildProviders(),
                job.getTask(),
                to,
                job.getVariants(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    @Override
    TestJob transform(TestJob job) {
        final Platform platform = isFromPlatform(job.getPlatform()) ? to : job.getPlatform();
        final Platform buildPlatform = isFromBuildPlatform(job.getBuildPlatform()) ? to : job.getBuildPlatform();
        return new TestJob(
                job.getPlatformProvider(),
                job.getProjectName(),
                job.getProjectType(),
                job.getProduct(),
                job.getJdkVersion(),
                job.getBuildProviders(),
                job.getTask(),
                platform,
                job.getVariants(),
                buildPlatform,
                job.getBuildPlatformProvider(),
                job.getBuildTask(),
                job.getBuildVariants(),
                job.getProjectSubpackageBlacklist(),
                job.getProjectSubpackageWhitelist(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    private boolean isFromPlatform(final Platform platform) {
        return platform.getId().equals(from.getId());
    }

    private boolean isFromBuildPlatform(final Platform platform) {
        return platform != null && isFromPlatform(platform);
    }
}
