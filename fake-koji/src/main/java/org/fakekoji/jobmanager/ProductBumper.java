package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.Product;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.JDKVersion;

public class ProductBumper extends JobModifier {
    private final JDKVersion fromJDKVersion;
    private final JDKVersion toJDKVersion;
    private final String fromPackageName;
    private final String toPackageName;

    public ProductBumper(
            final String fromPackageName,
            final String toPackageName,
            final JDKVersion fromJDKVersion,
            final JDKVersion toJDKVersion
    ) {
        this.fromPackageName = fromPackageName;
        this.toPackageName = toPackageName;
        this.fromJDKVersion = fromJDKVersion;
        this.toJDKVersion = toJDKVersion;
    }

    private boolean shouldJobPass(Job job) {
        return job.getProduct().getPackageName().equals(fromPackageName) && job.getJdkVersion().equals(fromJDKVersion);
    }

    @Override
    boolean shouldPass(PullJob job) {
        return this.shouldJobPass(job);
    }

    @Override
    boolean shouldPass(BuildJob job) {
        return this.shouldJobPass(job);
    }

    @Override
    boolean shouldPass(TestJob job) {
        return this.shouldJobPass(job);
    }

    @Override
    PullJob transform(PullJob job) {
        return new PullJob(
                job.getProjectName(),
                job.getRepoUrl(),
                new Product(
                        toJDKVersion.getId(),
                        toPackageName
                ),
                toJDKVersion,
                job.getRepositoriesRoot(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    @Override
    BuildJob transform(BuildJob job) {
        return new BuildJob(
                job.getPlatformProvider(),
                job.getProjectName(),
                new Product(
                        toJDKVersion.getId(),
                        toPackageName
                ),
                toJDKVersion,
                job.getBuildProviders(),
                job.getTask(),
                job.getPlatform(),
                job.getVariants(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }

    @Override
    TestJob transform(TestJob job) {
        return new TestJob(
                job.getPlatformProvider(),
                job.getProjectName(),
                job.getProjectType(),
                new Product(
                        toJDKVersion.getId(),
                        toPackageName
                ),
                toJDKVersion,
                job.getBuildProviders(),
                job.getTask(),
                job.getPlatform(),
                job.getVariants(),
                job.getBuildPlatform(),
                job.getBuildPlatformProvider(),
                job.getBuildTask(),
                job.getBuildVariants(),
                job.getProjectSubpackageBlacklist(),
                job.getProjectSubpackageWhitelist(),
                job.getScriptsRoot(),
                job.getProjectVariables()
        );
    }
}
