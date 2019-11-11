package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.storage.StorageException;

import java.io.IOException;
import java.util.Set;

public interface JobUpdater {

    JobUpdateResults update(JDKProject oldProject, JDKProject newProject) throws StorageException, ManagementException;
}
