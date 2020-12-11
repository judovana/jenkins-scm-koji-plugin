package org.fakekoji.jobmanager;

import org.fakekoji.DataGenerator;
import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.model.JobCollisionAction;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.Platform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class JobModifierTest {

    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private AccessibleSettings settings;
    private JobModifier modifier;

    public void setup() throws IOException {
        JenkinsCliWrapper.killCli();
        settings = DataGenerator.getSettings(DataGenerator.initFolders(temporaryFolder));
        DataGenerator.createProjectJobs(settings);
        modifier = new JobModifier(settings) {
            @Override
            boolean shouldPass(TestJob job) {
                final Platform f30 = DataGenerator.getF30x64();
                return job.getPlatform().equals(f30);
            }

            @Override
            TestJob transform(TestJob job) {
                return new TestJob(
                        job.getPlatformProvider(),
                        job.getProjectName(),
                        job.getProjectType(),
                        job.getProduct(),
                        job.getJdkVersion(),
                        job.getBuildProviders(),
                        job.getTask(),
                        DataGenerator.getF31x64(),
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
        };
    }

    public void testCollisionWithStop() {
        final Result<JobUpdateResults, OToolError> result = modifier.modifyJobs(
                DataGenerator.getProjects(),
                JobCollisionAction.STOP,
                true
        );
        Assert.assertFalse(result.isError());
        Assert.assertEquals(4, result.getValue().jobsCreated.size());
        Assert.assertTrue(result.getValue().jobsCreated.stream().noneMatch(res -> res.success));
    }

    public void testCollisionWithKeepExisting() {
        final Result<JobUpdateResults, OToolError> result = modifier.modifyJobs(
                DataGenerator.getProjects(),
                JobCollisionAction.KEEP_EXISTING,
                true
        );
        Assert.assertFalse(result.isError());
        Assert.assertEquals(4, result.getValue().jobsCreated.size());
        Assert.assertTrue(result.getValue().jobsCreated.stream().allMatch(res -> res.success));
        final File jobs = settings.getJenkinsJobsRoot();
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.wayland.future.lnxagent.jfron").exists());
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.x11.legacy.lnxagent.jfron").exists());
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.x11.future.lnxagent.jfron").exists());
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-defaultgc.wayland.legacy.lnxagent.jfron").exists());


        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-shenandoah.wayland.future.lnxagent.jfron").exists());
        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-shenandoah.x11.legacy.lnxagent.jfron").exists());
        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-shenandoah.x11.future.lnxagent.jfron").exists());
        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-defaultgc.wayland.legacy.lnxagent.jfron").exists());
    }

    public void testCollisionWithKeepNew() {
        final Result<JobUpdateResults, OToolError> result = modifier.modifyJobs(
                DataGenerator.getProjects(),
                JobCollisionAction.KEEP_BUMPED,
                true
        );
        Assert.assertFalse(result.isError());
        Assert.assertEquals(4, result.getValue().jobsCreated.size());
        Assert.assertTrue(result.getValue().jobsCreated.stream().allMatch(res -> res.success));
        final File jobs = settings.getJenkinsJobsRoot();
        final File archive = settings.getJenkinsJobArchiveRoot();
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.wayland.future.lnxagent.jfron").exists());
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.x11.legacy.lnxagent.jfron").exists());
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.x11.future.lnxagent.jfron").exists());
        Assert.assertFalse(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-defaultgc.wayland.legacy.lnxagent.jfron").exists());

        Assert.assertTrue(new File(archive, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.wayland.future.lnxagent.jfron").exists());
        Assert.assertTrue(new File(archive, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.x11.legacy.lnxagent.jfron").exists());
        Assert.assertTrue(new File(archive, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-shenandoah.x11.future.lnxagent.jfron").exists());
        Assert.assertTrue(new File(archive, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f30.x86_64.vagrant-defaultgc.wayland.legacy.lnxagent.jfron").exists());

        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-shenandoah.wayland.future.lnxagent.jfron").exists());
        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-shenandoah.x11.legacy.lnxagent.jfron").exists());
        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-shenandoah.x11.future.lnxagent.jfron").exists());
        Assert.assertTrue(new File(jobs, "tck-jdk8-uName-f29.x86_64-release.hotspot.sdk-f31.x86_64.vagrant-defaultgc.wayland.legacy.lnxagent.jfron").exists());

    }
}
