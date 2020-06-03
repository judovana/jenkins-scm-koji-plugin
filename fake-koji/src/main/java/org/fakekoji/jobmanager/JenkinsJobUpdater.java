package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.model.TaskJob;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JenkinsJobUpdater implements JobUpdater {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    static final String JENKINS_JOB_CONFIG_FILE = "config.xml";

    private final ConfigManager configManager;
    private final JDKProjectParser jdkProjectParser;
    private final File jenkinsJobsRoot;
    private final File jenkinsJobArchiveRoot;

    public JenkinsJobUpdater(
            final ConfigManager configManager,
            final JDKProjectParser jdkProjectParser,
            final File jenkinsJobsRoot,
            final File jenkinsJobArchiveRoot

    ) {
        this.configManager = configManager;
        this.jdkProjectParser = jdkProjectParser;
        this.jenkinsJobsRoot = jenkinsJobsRoot;
        this.jenkinsJobArchiveRoot = jenkinsJobArchiveRoot;
    }

    @Override
    public JobUpdateResults regenerate(Project project, String whitelist) throws StorageException, ManagementException {
        final Set<Job> jobs = project == null ? Collections.emptySet() : jdkProjectParser.parse(project);
        return regenerate(jobs, whitelist);
    }

    @Override
    public JobUpdateResults update(Project oldProject, Project newProject) throws StorageException, ManagementException {
        final Set<Job> oldJobs;
        final Set<Job> newJobs;
        oldJobs = oldProject == null ? Collections.emptySet() : jdkProjectParser.parse(oldProject);
        newJobs = newProject == null ? Collections.emptySet() : jdkProjectParser.parse(newProject);

        return update(oldJobs, newJobs);
    }

    @Override
    public JobUpdateResults update(Platform platform) throws StorageException {
        wakeUpJenkins();
        final Predicate<Job> platformJobPredicate = job ->
                job instanceof TaskJob && ((TaskJob) job).getPlatform().getId().equals(platform.getId());
        final List<JobUpdateResult> jobsRewritten = update(platformJobPredicate, jobUpdateFunctionWrapper(getRewriteFunction()));
        return new JobUpdateResults(
                Collections.emptyList(),
                Collections.emptyList(),
                jobsRewritten,
                Collections.emptyList()
        );
    }

    @Override
    public JobUpdateResults update(Task task) throws StorageException {
        wakeUpJenkins();
        final Predicate<Job> taskJobPredicate = job ->
                job instanceof TaskJob && ((TaskJob) job).getTask().getId().equals(task.getId());
        final List<JobUpdateResult> jobsRewritten = update(taskJobPredicate, jobUpdateFunctionWrapper(getRewriteFunction()));
        return new JobUpdateResults(
                Collections.emptyList(),
                Collections.emptyList(),
                jobsRewritten,
                Collections.emptyList()
        );
    }

    public static void wakeUpJenkins() {
        try {
            //ping the cli, to avoid first call sometimes to fail
            JenkinsCliWrapper.getCli().listJobsToArray();
        } catch (Throwable e) {
            //ignoring
        }
    }

    private List<JobUpdateResult> update(
            final Predicate<Job> jobPredicate,
            final Function<Job, JobUpdateResult> jobUpdateFunction
    ) throws StorageException {

        final Set<Job> jobs = Stream.of(
                configManager.jdkProjectManager.readAll(),
                configManager.jdkTestProjectManager.readAll()
        )
                .flatMap(Collection::stream)
                .map(jdkProject -> {
                    try {
                        return jdkProjectParser.parse(jdkProject);
                    } catch (ManagementException | StorageException e) {
                        LOGGER.severe("Failed to parse JDK project " + jdkProject.getId() + ": " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return jobs.stream()
                .filter(jobPredicate)
                .map(jobUpdateFunction)
                .collect(Collectors.toList());
    }

    /**
     * Regenerate job
     * Unlike update, this one do not delete
     * Unlike update, it determines its action based on  jobs, not old projects
     *
     * @param jobs
     * @return
     */
    JobUpdateResults regenerate(Set<Job> jobs, String whitelist) {
        if (whitelist == null || whitelist.trim().isEmpty()) {
            whitelist = ".*";
        }
        Pattern whitelistPattern = Pattern.compile(whitelist);
        final Function<Job, JobUpdateResult> rewriteFunction = jobUpdateFunctionWrapper(getRewriteFunction());
        final Function<Job, JobUpdateResult> createFunction = jobUpdateFunctionWrapper(getCreateFunction());
        final Function<Job, JobUpdateResult> reviveFunction = jobUpdateFunctionWrapper(getReviveFunction());

        final List<JobUpdateResult> jobsCreated = new LinkedList<>();
        final List<JobUpdateResult> jobsRewritten = new LinkedList<>();
        final List<JobUpdateResult> jobsRevived = new LinkedList<>();

        final Set<String> archivedJobs = new HashSet<>(Arrays.asList(Objects.requireNonNull(jenkinsJobArchiveRoot.list())));
        final Set<String> existingJobs = new HashSet<>(Arrays.asList(Objects.requireNonNull(jenkinsJobsRoot.list())));

        for (final Job job : jobs) {
            if (!whitelistPattern.matcher(job.getName()).matches()) {
                continue;
            }
            if (archivedJobs.contains(job.toString()) && existingJobs.contains(job.toString())) {
                ///very wierd!
                jobsRewritten.add(rewriteFunction.apply(job));
            } else if (archivedJobs.contains(job.toString())) {
                jobsRevived.add(reviveFunction.apply(job));
            } else if (existingJobs.contains(job.toString())) {
                jobsRewritten.add(rewriteFunction.apply(job));
            } else {
                jobsCreated.add(createFunction.apply(job));
            }
        }

        return new JobUpdateResults(
                jobsCreated,
                new LinkedList<JobUpdateResult>(),
                jobsRewritten,
                jobsRevived
        );
    }

    /**
     * Regenerate all jobs. If job is mssing, is (re)created.
     *
     * @throws StorageException
     */
    public <T extends Project> JobUpdateResults regenerateAll(
            String projectId,
            Manager<T> projectManager,
            String whitelist
    ) throws StorageException, ManagementException {
        JobUpdateResults sum = new JobUpdateResults();
        JenkinsJobUpdater.wakeUpJenkins();
        final List<T> projects = projectManager.readAll();
        for (final Project project : projects) {
            if (projectId == null || project.getId().equals(projectId)) {
                JobUpdateResults r = regenerate(project, whitelist);
                sum = sum.add(r);
            }

        }
        return sum;
    }

    JobUpdateResults update(Set<Job> oldJobs, Set<Job> newJobs) {

        final Function<Job, JobUpdateResult> rewriteFunction = jobUpdateFunctionWrapper(getRewriteFunction());
        final Function<Job, JobUpdateResult> createFunction = jobUpdateFunctionWrapper(getCreateFunction());
        final Function<Job, JobUpdateResult> archiveFunction = jobUpdateFunctionWrapper(getArchiveFunction());
        final Function<Job, JobUpdateResult> reviveFunction = jobUpdateFunctionWrapper(getReviveFunction());

        final List<JobUpdateResult> jobsCreated = new LinkedList<>();
        final List<JobUpdateResult> jobsArchived = new LinkedList<>();
        final List<JobUpdateResult> jobsRewritten = new LinkedList<>();
        final List<JobUpdateResult> jobsRevived = new LinkedList<>();

        final Set<String> archivedJobs = new HashSet<>(Arrays.asList(Objects.requireNonNull(jenkinsJobArchiveRoot.list())));

        wakeUpJenkins();

        for (final Job job : oldJobs) {
            if (newJobs.stream().noneMatch(newJob -> job.toString().equals(newJob.toString()))) {
                jobsArchived.add(archiveFunction.apply(job));
            }
        }
        for (final Job job : newJobs) {
            if (archivedJobs.contains(job.toString())) {
                jobsRevived.add(reviveFunction.apply(job));
                continue;
            }
            final Optional<Job> optional = oldJobs.stream()
                    .filter(oldJob -> job.toString().equals(oldJob.toString()))
                    .findAny();
            if (optional.isPresent()) {
                final Job oldJob = optional.get();
                if (!oldJob.equals(job)) {
                    jobsRewritten.add(rewriteFunction.apply(job));
                }
                continue;
            }
            jobsCreated.add(createFunction.apply(job));
        }
        return new JobUpdateResults(
                jobsCreated,
                jobsArchived,
                jobsRewritten,
                jobsRevived
        );
    }

    public JobUpdateResults bump(final Set<Tuple<Job, Job>> jobTuples) {
        return new JobUpdateResults(
                jobTuples.stream()
                        .map(jobUpdateFunctionWrapper(getBumpFunction()))
                        .collect(Collectors.toList()),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public List<JobUpdateResult> checkBumpJobs(final Set<Tuple<Job, Job>> jobTuples) {
        final Set<String> jobNames = Stream.of(Objects.requireNonNull(jenkinsJobsRoot.list()))
                .collect(Collectors.toSet());
        return jobTuples.stream()
                .filter(jobTuple -> jobNames.contains(jobTuple.y.getName()))
                .map(jobTuple -> new JobUpdateResult(
                        jobTuple.x.getName() + " => " + jobTuple.y.getName(),
                        false,
                        jobTuple.y + " already exists"
                ))
                .collect(Collectors.toList());
    }

    private <T> Function<T, JobUpdateResult> jobUpdateFunctionWrapper(JobUpdateFunction<T> updateFunction) {
        return job -> {
            try {
                return updateFunction.apply(job);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return new JobUpdateResult(job.toString(), false, e.getMessage());
            }
        };
    }

    private JobUpdateFunction<Job> getCreateFunction() {
        return job -> {
            final String jobName = job.toString();
            LOGGER.info("Creating job " + jobName);
            final String jobsRootPath = jenkinsJobsRoot.getAbsolutePath();
            LOGGER.info("Creating directory " + jobName + " in " + jobsRootPath);
            final File jobDir = Paths.get(jobsRootPath, jobName).toFile();
            if (!jobDir.mkdir()) {
                throw new IOException("Could't create file: " + jobDir.getAbsolutePath());
            }
            final String jobDirPath = jobDir.getAbsolutePath();
            LOGGER.info("Creating file " + JENKINS_JOB_CONFIG_FILE + " in " + jobDirPath);
            return new PrimaryExceptionThrower<JobUpdateResult>(
                    () -> {
                        Utils.writeToFile(
                                Paths.get(jobDirPath, JENKINS_JOB_CONFIG_FILE),
                                job.generateTemplate()
                        );
                    }, () -> {
                createManuallyUploadedJob(jobName);
            }, new JobUpdateResult(jobName, true)).call();
        };
    }

    private JobUpdateFunction<Job> getReviveFunction() {
        return job -> {
            final String jobName = job.toString();
            final File src = Paths.get(jenkinsJobArchiveRoot.getAbsolutePath(), job.toString()).toFile();
            final File dst = Paths.get(jenkinsJobsRoot.getAbsolutePath(), job.toString()).toFile();
            LOGGER.info("Reviving job " + jobName);
            LOGGER.info("Moving directory " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
            return new PrimaryExceptionThrower<JobUpdateResult>(
                    () -> {
                        Utils.moveDirByCopy(src, dst);
                        //regenerate conig
                        LOGGER.info("recreating file " + JENKINS_JOB_CONFIG_FILE + " in " + dst);
                        Utils.writeToFile(
                                Paths.get(dst.getAbsolutePath(), JENKINS_JOB_CONFIG_FILE),
                                job.generateTemplate());
                    }, () -> {
                createManuallyUploadedJob(jobName);
            }, new JobUpdateResult(jobName, true)).call();
        };
    }

    private JobUpdateFunction<Job> getArchiveFunction() {
        return job -> {
            final String jobName = job.toString();
            final File src = Paths.get(jenkinsJobsRoot.getAbsolutePath(), job.toString()).toFile();
            final File dst = Paths.get(jenkinsJobArchiveRoot.getAbsolutePath(), job.toString()).toFile();
            LOGGER.info("Archiving job " + jobName);
            LOGGER.info("Moving directory " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
            Utils.moveDirByCopy(src, dst);
            //we delte only if archivation suceed
            JenkinsCliWrapper.getCli().deleteJobs(jobName).throwIfNecessary();
            return new JobUpdateResult(jobName, true);
        };
    }

    private JobUpdateFunction<Job> getRewriteFunction() {
        return job -> {
            final String jobName = job.toString();
            final File jobConfig = Paths.get(jenkinsJobsRoot.getAbsolutePath(), jobName, JENKINS_JOB_CONFIG_FILE).toFile();
            LOGGER.info("Rewriting job " + jobName);
            LOGGER.info("Writing to file " + jobConfig.getAbsolutePath());
            return new PrimaryExceptionThrower<JobUpdateResult>(
                    () -> {
                        Utils.writeToFile(jobConfig, job.generateTemplate());
                    }, () -> {
                updateManuallyUpdatedJob(jobName);
            }, new JobUpdateResult(jobName, true)).call();
        };
    }

    private JobUpdateFunction<Tuple<Job, Job>> getBumpFunction() {
        final File jobsRoot = jenkinsJobsRoot;
        return jobTuple -> {
            final Job original = jobTuple.x;
            final Job transformed = jobTuple.y;
            final String originalName = original.getName();
            final File originalDir = Paths.get(jobsRoot.getAbsolutePath(), originalName).toFile();
            final String transformedName = transformed.getName();
            final File transformedDir = Paths.get(jobsRoot.getAbsolutePath(), transformedName).toFile();
            LOGGER.info("Bumping job " + originalName + " to " + transformedName);
            LOGGER.info("Moving directory " + originalDir.getAbsolutePath() + " to " + transformedDir.getAbsolutePath());
            Utils.moveDir(originalDir, transformedDir); //if exception is thrown from here, better to die with it
            IOException deleteJobException = null;
            try {
                JenkinsCliWrapper.getCli().deleteJobs(originalName).throwIfNecessary();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                deleteJobException = ex;
            }
            final File jobConfig = Paths.get(transformedDir.getAbsolutePath(), JENKINS_JOB_CONFIG_FILE).toFile();
            LOGGER.info("Rewriting config of " + transformedName);
            IOException newCfgException = null;
            try {
                Utils.writeToFile(jobConfig, transformed.generateTemplate());
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                newCfgException = ex;
            }
            IOException createJobException = null;
            try {
                JenkinsCliWrapper.getCli().createManuallyUploadedJob(jobsRoot.getAbsoluteFile(),transformedName).throwIfNecessary();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                createJobException = ex;
            }
            if (deleteJobException == null && newCfgException == null && createJobException == null) {
                return new JobUpdateResult(transformedName, true, "bumped from " + originalName + " to " + transformedName);
            } else {
                String s = "Exception(s) on the fly:";
                if (deleteJobException != null) {
                    s = s + " 1) deleteJobException " + deleteJobException.getMessage();
                }
                if (newCfgException != null) {
                    s = s + " 2) newCfgException " + newCfgException.getMessage();
                }
                if (createJobException != null) {
                    s = s + " 3) createJobException  " + createJobException.getMessage();
                }
                return new JobUpdateResult(transformedName, false, "bump from " + originalName + " to " + transformedName + " failed. See logs. " + s);
            }
        };
    }

    private void createManuallyUploadedJob(final String jobName) throws Exception {
        JenkinsCliWrapper.getCli().createManuallyUploadedJob(jenkinsJobsRoot, jobName).throwIfNecessary();
        ;
    }

    private void updateManuallyUpdatedJob(final String jobName) throws Exception {
        JenkinsCliWrapper.getCli().updateManuallyUpdatedJob(jenkinsJobsRoot, jobName).throwIfNecessary();
        ;
    }

    private interface JobUpdateFunction<T> {

        JobUpdateResult apply(T t) throws Exception;
    }

    static interface Rummable {

        public void rum() throws Exception;
    }

    static class PrimaryExceptionThrower<T> {

        private final Rummable mainCall;
        private final Rummable finalCall;
        private final T result;

        public PrimaryExceptionThrower(Rummable mainCall, Rummable finalCall, T result) {
            this.mainCall = mainCall;
            this.finalCall = finalCall;
            this.result = result;
        }

        public synchronized T call() throws Exception {
            Exception saved = null;
            try {
                mainCall.rum();
            } catch (Exception mainEx) {
                saved = mainEx;
            } finally {
                try {
                    finalCall.rum();
                } catch (Exception secondary) {
                    if (saved != null) {
                        LOGGER.log(Level.SEVERE, secondary.getMessage(), secondary);
                        throw saved;
                    } else {
                        throw secondary;
                    }
                }
            }
            if (saved != null) {
                throw saved;
            }
            return result;
        }
    }
}
