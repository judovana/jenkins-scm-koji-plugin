package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.Job;

import java.io.IOException;
import java.util.Set;

public interface JobUpdater {

    void update(Set<Job> oldJobs, Set<Job> newJobs) throws IOException;
}
