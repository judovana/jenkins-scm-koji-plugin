package org.fakekoji.jobmanager;

import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResult;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;

import java.util.List;
import java.util.Set;

public interface JobUpdater {

    JobUpdateResults update(Project oldProject, Project newProject) throws StorageException, ManagementException;

    JobUpdateResults regenerate(Project project) throws StorageException, ManagementException;

    <T extends Project> JobUpdateResults regenerateAll(
            String projectId,
            Manager<T> projectManager
    ) throws StorageException, ManagementException;

    JobUpdateResults update(Platform platform) throws StorageException;

    JobUpdateResults update(Task task) throws StorageException;

    List<JobUpdateResult> checkBumpJobs(final Set<Tuple<Job, Job>> jobTuples);

    JobUpdateResults bump(final Set<Tuple<Job, Job>> jobTuple);
}
