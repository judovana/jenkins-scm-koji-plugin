package org.fakekoji.jobmanager;

import org.fakekoji.jobmanager.model.JobUpdateResults;

import java.util.Optional;

public class BumpResult {
    private final JobUpdateResults jobResults;
    private final BuildDirUpdater.BuildUpdateSummary buildUpdateSummary;
    private final String message;

    public BumpResult(
            final JobUpdateResults jobResults,
            final BuildDirUpdater.BuildUpdateSummary buildUpdateSummary,
            final String message
    ) {
        this.jobResults = jobResults;
        this.buildUpdateSummary = buildUpdateSummary;
        this.message = message == null ? "" : message;
    }
    
    public BumpResult(JobUpdateResults jobResults) {
        this(jobResults, null, null);
    }

    public BumpResult(
            final JobUpdateResults jobResults,
            final BuildDirUpdater.BuildUpdateSummary buildUpdateSummary
    ) {
        this(jobResults, buildUpdateSummary, null);
    }
    
    public BumpResult(
            final JobUpdateResults jobResults,
            final String message
    ) {
        this(jobResults, null, message);
    }
    

    public JobUpdateResults getJobResults() {
        return jobResults;
    }

    public Optional<BuildDirUpdater.BuildUpdateSummary> getBuildUpdateSummary() {
        return Optional.ofNullable(buildUpdateSummary);
    }
    
    public String getMessage() {
        return message;
    }
}
