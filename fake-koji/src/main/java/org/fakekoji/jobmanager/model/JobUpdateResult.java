package org.fakekoji.jobmanager.model;

import java.util.Objects;

public class JobUpdateResult {

    public final String jobName;
    public final boolean success;
    public final String message;

    public JobUpdateResult(String jobName, boolean success, String message) {
        this.jobName = jobName;
        this.success = success;
        this.message = message;
    }

    public JobUpdateResult(String jobName, boolean success) {
        this.jobName = jobName;
        this.success = success;
        this.message = "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobUpdateResult)) return false;
        JobUpdateResult that = (JobUpdateResult) o;
        return success == that.success &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobName, success, message);
    }

    @Override
    public String toString() {
        return "JobUpdateResult{" +
                "jobName='" + jobName + '\'' +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
