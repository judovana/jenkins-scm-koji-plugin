package org.fakekoji.jobmanager.bumpers;

import org.fakekoji.api.http.rest.OToolError;
import org.fakekoji.api.http.rest.args.BumpArgs;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.BuildJob;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobBump;
import org.fakekoji.jobmanager.model.JobCollisionAction;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.PullJob;
import org.fakekoji.jobmanager.model.TestJob;
import org.fakekoji.storage.StorageException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class JobModifier {

    private final AccessibleSettings settings;

    public JobModifier(final AccessibleSettings settings) {
        this.settings = settings;
    }

    public Result<JobUpdateResults, OToolError> modifyJobs(final Collection<Project> projects) {
        return modifyJobs(projects, JobCollisionAction.STOP, true);
    }

    public Result<JobUpdateResults, OToolError> modifyJobs(
            final Collection<Project> projects,
            final BumpArgs bumpArgs
    ) {
        return modifyJobs(projects, bumpArgs.action, bumpArgs.execute);
    }

    public Result<JobUpdateResults, OToolError> modifyJobs(
            final Collection<Project> projects,
            final JobCollisionAction jobCollisionAction,
            final boolean execute
    ) {
        final Set<Job> jobs = new HashSet<>();
        try {
            for (final Project project : projects) {
                final Set<Job> projectJobs = settings.getJdkProjectParser().parse(project);
                jobs.addAll(projectJobs);
            }
            final Set<Tuple<Job, Optional<Job>>> jobTuples = jobs.stream()
                    .map(getTransformFunction())
                    .collect(Collectors.toSet());
            final Set<JobBump> jobsToBump = jobTuples.stream()
                    .filter(jobTuple -> jobTuple.y.isPresent())
                    .map(jobTuple -> new Tuple<>(jobTuple.x, jobTuple.y.get()))
                    .map(settings.getJobUpdater().getCollisionCheck())
                    .collect(Collectors.toSet());
            if (!execute) {
                return Result.ok(new JobUpdateResults(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        jobsToBump.stream().map(jobBump -> new JobUpdateResult(
                                jobBump.from.getName() + " => " + jobBump.to.getName(), true
                        )).collect(Collectors.toList()),
                        Collections.emptyList()
                ));
            }
            final Set<Tuple<Job, Job>> collisions = jobsToBump.stream()
                    .filter(jobBump -> jobBump.isCollision)
                    .map(triple -> new Tuple<>(triple.from, triple.to))
                    .collect(Collectors.toSet());
            if (!collisions.isEmpty() && jobCollisionAction.equals(JobCollisionAction.STOP)) {
                return Result.ok(new JobUpdateResults(
                        collisions.stream().map(jobTuple -> new JobUpdateResult(
                                jobTuple.x.getName() + " => " + jobTuple.y.getName(),
                                false,
                                jobTuple.y + " already exists"
                        )).collect(Collectors.toList()),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
            }
            final Set<Job> finalJobs = jobTuples.stream()
                    .map(tuple -> tuple.y.orElseGet(() -> tuple.x))
                    .collect(Collectors.toSet());
            final Map<String, Set<Job>> jobMap = new HashMap<>();
            for (final Job job : finalJobs) {
                final String projectName = job.getProjectName();
                if (!jobMap.containsKey(projectName)) {
                    jobMap.put(projectName, new HashSet<>());
                }
                final Set<Job> projectJobs = jobMap.get(projectName);
                projectJobs.add(job);
            }
            final List<Project> assembledProjects = new ArrayList<>();
            for (final Set<Job> projectJobs : jobMap.values()) {
                final Result<Project, String> result = settings.getReverseJDKProjectParser().parseJobs(projectJobs);
                if (result.isError()) {
                    return Result.err(new OToolError(result.getError(), 500));
                } else {
                    assembledProjects.add(result.getValue());
                }
            }
            for (final Project project : assembledProjects) {
                final String id = project.getId();
                switch (project.getType()) {
                    case JDK_PROJECT:
                        settings.getConfigManager().jdkProjectManager.update(id, (JDKProject) project);
                        break;
                    case JDK_TEST_PROJECT:
                        settings.getConfigManager().jdkTestProjectManager.update(id, (JDKTestProject) project);
                        break;
                }
            }
            JenkinsJobUpdater.wakeUpJenkins();
            return Result.ok(settings.getJobUpdater().bump(jobsToBump, jobCollisionAction));

        } catch (StorageException e) {
            return Result.err(new OToolError(e.getMessage(), 500));
        } catch (ManagementException e) {
            return Result.err(new OToolError(e.getMessage(), 400));
        }
    }

    Function<Job, Tuple<Job, Optional<Job>>> getTransformFunction() {
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
