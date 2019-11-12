package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.storage.StorageException;

public interface JobUpdater {

    JobUpdateResults update(JDKProject oldProject, JDKProject newProject) throws StorageException, ManagementException;

    JobUpdateResults update(Platform platform) throws StorageException;

    JobUpdateResults update(Task task) throws StorageException;
}
