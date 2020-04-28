package org.fakekoji.jobmanager;

import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TestJob;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class JobModifier {

    public Function<Job, Tuple<Job, Optional<Job>>> getTransformFunction() {
        return job -> {
            final Optional<Job> transformed;
            if (job instanceof PullJob) {
                final PullJob pullJob = (PullJob) job;
                if (shouldPass(pullJob)) {
                    transformed = Optional.of(transform(pullJob));
                } else {
                    transformed = Optional.empty();
                }
            } else if (job instanceof BuildJob) {
                final BuildJob buildJob = (BuildJob) job;
                if (shouldPass(buildJob)) {
                    transformed = Optional.of(transform(buildJob));
                } else {
                    transformed = Optional.empty();
                }
            } else if (job instanceof TestJob) {
                final TestJob testJob = (TestJob) job;
                if (shouldPass(testJob)) {
                    transformed = Optional.of(transform(testJob));
                } else {
                    transformed = Optional.empty();
                }
            } else {
                throw new RuntimeException("Unknown type of job: " + job);
            }
            return new Tuple<>(job, transformed);
        };
    }

    Consumer<Tuple<Job, Job>> getSideEffectFunction() {
        return job -> {
        };
    }

    boolean shouldPass(PullJob job) {
        return false;
    }

    boolean shouldPass(BuildJob job) {
        return false;
    }

    boolean shouldPass(TestJob job) {
        return false;
    }

    PullJob transform(PullJob job) {
        return job;
    }

    BuildJob transform(BuildJob job) {
        return job;
    }

    TestJob transform(TestJob job) {
        return job;
    }
}
