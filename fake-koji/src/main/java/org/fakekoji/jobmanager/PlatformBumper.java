package org.fakekoji.jobmanager;

import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.PlatformBumpVariant;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.Platform;

public class PlatformBumper extends JobModifier {

    private final Platform from;
    private final Platform to;
    private final PlatformBumpVariant variant;

    public PlatformBumper(
            final AccessibleSettings settings,
            final Platform from,
            final Platform to,
            final PlatformBumpVariant variant
    ) {
        super(settings);
        this.from = from;
        this.to = to;
        this.variant = variant;
    }

    @Override
    boolean shouldPass(BuildJob job) {
        switch (variant) {
            case BOTH:
            case BUILD_ONLY:
                return isFromPlatform(job.getPlatform());
            case TEST_ONLY:
            default:
                return false;
        }
    }

    @Override
    boolean shouldPass(TestJob job) {
        switch (variant) {
            case BOTH:
                return isFromPlatform(job.getPlatform()) || isFromBuildPlatform(job.getBuildPlatform());
            case BUILD_ONLY:
                return isFromBuildPlatform(job.getBuildPlatform());
            case TEST_ONLY:
                return isFromPlatform(job.getPlatform());
            default:
                return false;
        }
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
        final Platform platform;
        final Platform buildPlatform;
        switch (variant) {
            case BOTH:
                platform = isFromPlatform(job.getPlatform()) ? to : job.getPlatform();
                buildPlatform = isFromBuildPlatform(job.getBuildPlatform()) ? to : job.getBuildPlatform();
                break;
            case BUILD_ONLY:
                platform = job.getPlatform();
                buildPlatform = isFromBuildPlatform(job.getBuildPlatform()) ? to : job.getBuildPlatform();
                break;
            case TEST_ONLY:
                platform = isFromPlatform(job.getPlatform()) ? to : job.getPlatform();
                buildPlatform = job.getBuildPlatform();
                break;
            default:
                return job;
        }
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
