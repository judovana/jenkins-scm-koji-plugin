package org.fakekoji.model;

import java.util.Objects;

public class OToolBuild {

    private final String packageName;
    private final String version;
    private final String changeSet;
    private final String garbage;
    private final String projectName;

    public OToolBuild(
            String packageName,
            String version,
            String changeSet,
            String garbage,
            String projectName
    ) {
        this.packageName = packageName;
        this.version = version;
        this.changeSet = changeSet;
        this.garbage = garbage;
        this.projectName = projectName;
    }

    public String getRelease() {
        final String garbage = this.garbage.equals("") ? "." : '.' + this.garbage + '.';
        return changeSet + garbage + projectName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public String getChangeSet() {
        return changeSet;
    }

    public String getGarbage() {
        return garbage;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OToolBuild)) return false;
        OToolBuild that = (OToolBuild) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(version, that.version) &&
                Objects.equals(changeSet, that.changeSet) &&
                Objects.equals(garbage, that.garbage) &&
                Objects.equals(projectName, that.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, version, changeSet, garbage, projectName);
    }

    @Override
    public String toString() {
        return "OToolBuild{" +
                "packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                ", changeSet='" + changeSet + '\'' +
                ", garbage='" + garbage + '\'' +
                ", projectName='" + projectName + '\'' +
                '}';
    }
}
