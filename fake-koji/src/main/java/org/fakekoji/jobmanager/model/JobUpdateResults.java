package org.fakekoji.jobmanager.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JobUpdateResults {

    public final List<JobUpdateResult> jobsCreated;
    public final List<JobUpdateResult> jobsArchived;
    public final List<JobUpdateResult> jobsRewritten;
    public final List<JobUpdateResult> jobsRevived;

    public JobUpdateResults() {
        jobsCreated = Collections.emptyList();
        jobsArchived = Collections.emptyList();
        jobsRewritten = Collections.emptyList();
        jobsRevived = Collections.emptyList();
    }

    public JobUpdateResults(
            List<JobUpdateResult> jobsCreated,
            List<JobUpdateResult> jobsArchived,
            List<JobUpdateResult> jobsRewritten,
            List<JobUpdateResult> jobsRevived
    ) {
        this.jobsCreated = jobsCreated;
        this.jobsArchived = jobsArchived;
        this.jobsRewritten = jobsRewritten;
        this.jobsRevived = jobsRevived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobUpdateResults)) return false;
        JobUpdateResults that = (JobUpdateResults) o;
        return Objects.equals(jobsCreated, that.jobsCreated) &&
                Objects.equals(jobsArchived, that.jobsArchived) &&
                Objects.equals(jobsRewritten, that.jobsRewritten) &&
                Objects.equals(jobsRevived, that.jobsRevived);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobsCreated, jobsArchived, jobsRewritten, jobsRevived);
    }

    @Override
    public String toString() {
        return "JobUpdateResults{" +
                "jobsCreated=" + jobsCreated +
                ", jobsArchived=" + jobsArchived +
                ", jobsRewritten=" + jobsRewritten +
                ", jobsRevived=" + jobsRevived +
                '}';
    }
}
