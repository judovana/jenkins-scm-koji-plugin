package org.fakekoji.jobmanager.model;

public class JobBump {
    public final Job from;
    public final Job to;
    public final boolean isCollision;

    public JobBump(Job from, Job to, boolean isCollision) {
        this.from = from;
        this.to = to;
        this.isCollision = isCollision;
    }
}
