package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.storage.StorageException;

import java.util.List;

public class OTool {

    private final JobUpdater jobUpdater;

    public OTool(final JobUpdater jobUpdater) {
        this.jobUpdater = jobUpdater;
    }

    /**
     * Regenerate all jobs. If job is mssing, is (re)created.
     *
     * @throws StorageException
     */
    public <T extends Project> JobUpdateResults regenerateAll(
            String projectId,
            Manager<T> projectManager
    ) throws StorageException, ManagementException {
        JobUpdateResults sum = new JobUpdateResults();
        JenkinsJobUpdater.wakeUpJenkins();
        final List<T> projects = projectManager.readAll();
        for (final Project project : projects) {
            if (projectId == null || project.getId().equals(projectId)) {
                JobUpdateResults r = jobUpdater.regenerate(project);
                sum = sum.add(r);
            }

        }
        return sum;
    }

}
