package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class JenkinsJobUpdater implements JobUpdater {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    static final String JENKINS_JOB_CONFIG_FILE = "config.xml";

    private final File jobsRoot;
    private final File jobArchiveRoot;

    public JenkinsJobUpdater(File jobsRoot, File jobArchiveRoot) {
        this.jobsRoot = jobsRoot;
        this.jobArchiveRoot = jobArchiveRoot;
    }

    @Override
    public void update(Set<Job> oldJobs, Set<Job> newJobs) throws IOException {
        final Set<String> archivedJobs = new HashSet<>(Arrays.asList(jobArchiveRoot.list()));
        for (final Job job : oldJobs) {
            if (newJobs.stream().noneMatch(newJob -> job.toString().equals(newJob.toString()))) {
                archive(job);
            }
        }
        for (final Job job : newJobs) {
            if (archivedJobs.contains(job.toString())) {
                revive(job);
                continue;
            }
            final Optional<Job> optional = oldJobs.stream()
                    .filter(oldJob -> job.toString().equals(oldJob.toString()))
                    .findAny();
            if (optional.isPresent()) {
                final Job oldJob = optional.get();
                if (!oldJob.equals(job)) {
                    update(job);
                }
                continue;
            }
            create(job);
        }
    }

    private void create(Job job) throws IOException {
        final String jobName = job.toString();
        LOGGER.info("Creating job " + jobName);
        final String jobsRootPath = jobsRoot.getAbsolutePath();
        LOGGER.info("Creating directory " + jobName + " in " + jobsRootPath);
        final File jobDir = Paths.get(jobsRootPath, jobName).toFile();
        if (!jobDir.mkdir()) {
            throw new IOException("Could't create file: " + jobDir.getAbsolutePath());
        }
        final String jobDirPath = jobDir.getAbsolutePath();
        LOGGER.info("Creating file " + JENKINS_JOB_CONFIG_FILE + " in " + jobDirPath);
        Utils.writeToFile(
                Paths.get(jobDirPath, JENKINS_JOB_CONFIG_FILE),
                job.generateTemplate()
        );
    }

    private void revive(Job job) throws IOException {
        final String jobName = job.toString();
        final File src = Paths.get(jobArchiveRoot.getAbsolutePath(), job.toString()).toFile();
        final File dst = Paths.get(jobsRoot.getAbsolutePath(), job.toString()).toFile();
        LOGGER.info("Reviving job " + jobName);
        LOGGER.info("Moving directory " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
        Utils.moveFile(src, dst);
    }

    private void archive(Job job) throws IOException {
        final String jobName = job.toString();
        final File src = Paths.get(jobsRoot.getAbsolutePath(), job.toString()).toFile();
        final File dst = Paths.get(jobArchiveRoot.getAbsolutePath(), job.toString()).toFile();
        LOGGER.info("Archiving job " + jobName);
        LOGGER.info("Moving directory " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
        Utils.moveFile(src, dst);
    }

    private void update(Job job) throws IOException {
        final String jobName = job.toString();
        final File jobConfig = Paths.get(jobsRoot.getAbsolutePath(), jobName, JENKINS_JOB_CONFIG_FILE).toFile();
        LOGGER.info("Updating job " + jobName);
        LOGGER.info("Writing to file " + jobConfig.getAbsolutePath());
        Utils.writeToFile(jobConfig, job.generateTemplate());
    }
}
