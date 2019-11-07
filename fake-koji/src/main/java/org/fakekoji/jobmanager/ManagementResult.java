package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JobUpdateResults;

import java.util.Objects;

public class ManagementResult {

    public final Object config;
    public final JobUpdateResults jobUpdateResults;

    public ManagementResult(Object config, JobUpdateResults jobUpdateResults) {
        this.config = config;
        this.jobUpdateResults = jobUpdateResults;
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
