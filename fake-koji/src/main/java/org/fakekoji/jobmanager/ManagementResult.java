package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JobUpdateResults;

import java.util.Objects;

public class ManagementResult <C> {

    public final C config;
    public final JobUpdateResults jobUpdateResults;

    public ManagementResult(C config, JobUpdateResults jobUpdateResults) {
        this.config = config;
        this.jobUpdateResults = jobUpdateResults;
    }

    public ManagementResult(C config) {
        this.config = config;
        this.jobUpdateResults = new JobUpdateResults();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManagementResult)) return false;
        ManagementResult that = (ManagementResult) o;
        return Objects.equals(config, that.config) &&
                Objects.equals(jobUpdateResults, that.jobUpdateResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config, jobUpdateResults);
    }

    @Override
    public String toString() {
        return "ManagementResult{" +
                "config=" + config +
                ", jobupdateResults=" + jobUpdateResults +
                '}';
    }
}
