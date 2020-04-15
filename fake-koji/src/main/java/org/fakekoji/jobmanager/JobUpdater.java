package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;

public interface JobUpdater {

    JobUpdateResults update(Project oldProject, Project newProject) throws StorageException, ManagementException;

    JobUpdateResults regenerate(Project project, String whitelist) throws StorageException, ManagementException;

    JobUpdateResults update(Platform platform) throws StorageException;

    JobUpdateResults update(Task task) throws StorageException;
}
