package org.fakekoji.jobmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JobUpdateResults {

    public final List<JobUpdateResult> jobsCreated;
    public final List<JobUpdateResult> jobsArchived;
    public final List<JobUpdateResult> jobsRewritten;
    public final List<JobUpdateResult> jobsRevived;

    /**
     * Returns new instance, based on this, accumualted together with
     * @return
     */
    public JobUpdateResults add(JobUpdateResults toAdd) {
        List<JobUpdateResult> c =new ArrayList<>(jobsCreated.size()+toAdd.jobsCreated.size());
        List<JobUpdateResult> a =new ArrayList<>(jobsArchived.size()+toAdd.jobsArchived.size());
        List<JobUpdateResult> rw =new ArrayList<>(jobsRewritten.size()+toAdd.jobsRewritten.size());
        List<JobUpdateResult> re =new ArrayList<>(jobsRevived.size()+toAdd.jobsRevived.size());
        c.addAll(jobsCreated);
        c.addAll(toAdd.jobsCreated);
        a.addAll(jobsArchived);
        a.addAll(toAdd.jobsArchived);
        rw.addAll(jobsRewritten);
        rw.addAll(toAdd.jobsRewritten);
        re.addAll(jobsRevived);
        re.addAll(toAdd.jobsRevived);
        return new JobUpdateResults(c,a,rw, re);
    }

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
        this.jobsCreated = Collections.unmodifiableList(jobsCreated);
        this.jobsArchived = Collections.unmodifiableList(jobsArchived);
        this.jobsRewritten = Collections.unmodifiableList(jobsRewritten);
        this.jobsRevived = Collections.unmodifiableList(jobsRevived);
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
