package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.jobmanager.model.Job;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JenkinsJobUpdater implements JobUpdater {

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
        final File jobDir = Paths.get(jobsRoot.getAbsolutePath(), job.toString()).toFile();
        if (!jobDir.mkdir()) {
            throw new IOException("Could't create file: " + jobDir.getAbsolutePath());
        }
        Utils.writeToFile(
                Paths.get(jobDir.getAbsolutePath(), JENKINS_JOB_CONFIG_FILE),
                job.generateTemplate()
        );

    }

    private void revive(Job job) throws IOException {
        Utils.moveFile(
                Paths.get(jobArchiveRoot.getAbsolutePath(), job.toString()).toFile(),
                Paths.get(jobsRoot.getAbsolutePath(), job.toString()).toFile()
        );
    }

    private void archive(Job job) throws IOException {
        Utils.moveFile(
                Paths.get(jobsRoot.getAbsolutePath(), job.toString()).toFile(),
                Paths.get(jobArchiveRoot.getAbsolutePath(), job.toString()).toFile()
        );
    }

    private void update(Job job) throws IOException {
        Utils.writeToFile(
                Paths.get(jobsRoot.getAbsolutePath(), job.toString()),
                job.generateTemplate()
        );
    }
}
