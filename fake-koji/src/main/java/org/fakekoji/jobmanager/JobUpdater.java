package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.jobmanager.model.JobUpdateResults;

import java.io.IOException;
import java.util.Set;

public interface JobUpdater {

    JobUpdateResults update(Set<Job> oldJobs, Set<Job> newJobs);
}
