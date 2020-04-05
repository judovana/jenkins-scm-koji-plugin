package org.fakekoji.jobmanager;

import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;

import java.util.Set;

public interface JobUpdater {

    JobUpdateResults update(Project oldProject, Project newProject) throws StorageException, ManagementException;

    JobUpdateResults regenerate(Project project, String whitelist) throws StorageException, ManagementException;

    JobUpdateResults update(Platform platform) throws StorageException;

    JobUpdateResults update(Task task) throws StorageException;

    JobUpdateResults bump(final Set<Tuple<Job, Job>> jobTuple);
}
