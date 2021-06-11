package org.fakekoji.jobmanager.bumpers;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.bumpers.PlatformBumper;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.PlatformBumpVariant;
import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.model.Platform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlatformBumperTest {

    private AccessibleSettings settings;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(temporaryFolder);
    }

    @Test
    public void bumpJDKProjectJobsWithoutMatch() {
        final Set<Job> jobs = DataGenerator.getJDKProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(
                settings,
                DataGenerator.getF29x64(),
                DataGenerator.getRHEL7x64(),
                PlatformBumpVariant.BOTH,
                Optional.empty()
        );
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(0, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertEquals(jobs, tuples.stream().map(tuple -> tuple.y.orElse(tuple.x)).collect(Collectors.toSet()));
    }

    @Test
    public void bumpJDKProjectJobsWithMatch() {
        final Platform from = DataGenerator.getRHEL7x64();
        final Platform to = DataGenerator.getF29x64();
        final Set<Job> jobs = DataGenerator.getJDKProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(settings, from, to, PlatformBumpVariant.BOTH, Optional.empty());

        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(4, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertTrue(tuples.stream().allMatch(isOk(from, to)));
    }

    @Test
    public void bumpJDKProjectJobsWithMatchBuildOnly() {
        final Platform from = DataGenerator.getRHEL7x64();
        final Platform to = DataGenerator.getF29x64();
        final Set<Job> jobs = DataGenerator.getJDKProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(settings, from, to, PlatformBumpVariant.BUILD_ONLY, Optional.empty());

        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(4, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertTrue(tuples.stream().allMatch(tuple -> {
            if (tuple.x instanceof BuildJob) {
                // is build job
                final BuildJob buildJob = (BuildJob) tuple.x;
                if (!tuple.y.isPresent()) {
                    // if job is not bumped, then its platform should not be equal to 'from' platform
                    return !buildJob.getPlatform().equals(from);
                }
                final BuildJob bumped = (BuildJob) tuple.y.get();
                // if bumped job's platform equals to 'to' platform -> ok
                return bumped.getPlatform().equals(to);
            }
            if (tuple.x instanceof TestJob) {
                // id test job
                final TestJob testJob = (TestJob) tuple.x;
                if (!tuple.y.isPresent()) {
                    // if job is not bumped, then its platform should not be equal to 'from' platform
                    return !testJob.getBuildPlatform().equals(from);
                }
                final TestJob bumped = (TestJob) tuple.y.get();
                // if bumped job's platform equals to 'to' platform -> ok
                return bumped.getBuildPlatform().equals(to);
            }
            // if is neither build nor test, it should not be bumped
            return !tuple.y.isPresent();
        }));
    }

    @Test
    public void bumpJDKProjectJobsWithMatchTestOnly() {
        final Platform from = DataGenerator.getRHEL7x64();
        final Platform to = DataGenerator.getF29x64();
        final Set<Job> jobs = DataGenerator.getJDKProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(settings, from, to, PlatformBumpVariant.TEST_ONLY, Optional.empty());

        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(2, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertTrue(tuples.stream().allMatch(tuple -> {
            if (tuple.x instanceof TestJob) {
                // is test job
                final TestJob testJob = (TestJob) tuple.x;
                if (!tuple.y.isPresent()) {
                    // if job is not bumped, then its platform should not be equal to 'from' platform
                    return !testJob.getPlatform().equals(from);
                }
                final TestJob bumped = (TestJob) tuple.y.get();
                // bumped job's platform should equal to 'to' platform
                return bumped.getPlatform().equals(to);
            }
            // if is not test job, should not be bumped
            return !tuple.y.isPresent();
        }));
    }

    @Test
    public void bumpJDKTestProjectJobsWithoutMatch() {
        final Set<Job> jobs = DataGenerator.getJDKTestProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(
                settings,
                DataGenerator.getRHEL7Zx64(),
                DataGenerator.getRHEL7x64(),
                PlatformBumpVariant.BOTH,
                Optional.empty()
        );
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(0, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertEquals(jobs, tuples.stream().map(tuple -> tuple.y.orElse(tuple.x)).collect(Collectors.toSet()));
    }

    @Test
    public void bumpJDKTestProjectJobsWithAllMatching() {
        final Platform from = DataGenerator.getRHEL7x64();
        final Platform to = DataGenerator.getF29x64();
        final Set<Job> jobs = DataGenerator.getJDKTestProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(settings, from, to, PlatformBumpVariant.BOTH, Optional.empty());
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(5, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertTrue(tuples.stream().allMatch(isOk(from, to)));
    }

    @Test
    public void bumpJDKTestProjectJobsWithAllMatchingFiltered() {
        final Platform from = DataGenerator.getRHEL7x64();
        final Platform to = DataGenerator.getF29x64();
        final Set<Job> jobs = DataGenerator.getJDKTestProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(settings, from, to, PlatformBumpVariant.BOTH, Optional.of(Pattern.compile("tck.*")));
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(4, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertFalse(tuples.stream().allMatch(isOk(from, to)));
    }


    @Test
    public void bumpJDKTestProjectJobsWithSomeMatching() {
        final Platform from = DataGenerator.getF29x64();
        final Platform to = DataGenerator.getRHEL7x64();
        final Set<Job> jobs = DataGenerator.getJDKTestProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(settings, from, to, PlatformBumpVariant.BOTH, Optional.empty());
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(2, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertTrue(tuples.stream().allMatch(isOk(from, to)));
    }

    @Test
    public void bumpJDKTestProjectJobsWithSomeMatchingFitlered() {
        final Platform from = DataGenerator.getF29x64();
        final Platform to = DataGenerator.getRHEL7x64();
        final Set<Job> jobs = DataGenerator.getJDKTestProjectJobs();
        final PlatformBumper bumper = new PlatformBumper(settings, from, to, PlatformBumpVariant.BOTH, Optional.of(Pattern.compile(".*wayland.*")));
        final Set<Tuple<Job, Optional<Job>>> tuples = jobs.stream()
                .map(bumper.getTransformFunction())
                .collect(Collectors.toSet());
        Assert.assertEquals(1, tuples.stream().filter(tuple -> tuple.y.isPresent()).count());
        Assert.assertFalse(tuples.stream().allMatch(isOk(from, to)));
    }



    private Predicate<Tuple<Job, Optional<Job>>> isOk(final Platform from, final Platform to) {
        return tuple -> {
            final Job old = tuple.x;
            final Optional<Job> optionalJob = tuple.y;
            if (hasPlatform(old, from)) {
                if (!optionalJob.isPresent()) {
                    return false;
                }
                final Job bumped = optionalJob.get();
                if (!hasPlatform(bumped, to)) {
                    return false;
                }
            }
            if (hasBuildPlatform(old, from)) {
                if (!optionalJob.isPresent()) {
                    return false;
                }
                final Job bumped = optionalJob.get();
                if (!hasBuildPlatform(bumped, to)) {
                    return false;
                }
            }
            return true;
        };
    }

    private boolean hasPlatform(final Job job, final Platform platform) {
        if (!(job instanceof TaskJob)) {
            return false;
        }
        final TaskJob taskJob = (TaskJob) job;
        return taskJob.getPlatform().equals(platform);
    }

    private boolean hasBuildPlatform(final Job job, final Platform platform) {
        if (!(job instanceof TestJob)) {
            return false;
        }
        final TestJob testJob = (TestJob) job;
        if (testJob.getBuildPlatform() == null) {
            return false;
        }
        return testJob.getBuildPlatform().equals(platform);
    }
}
