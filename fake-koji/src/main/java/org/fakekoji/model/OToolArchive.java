package org.fakekoji.model;

import java.util.Objects;

public class OToolArchive extends OToolBuild {

    private final String archive;
    private final String suffix;

    public OToolArchive(
            String packageName,
            String version,
            String changeSet,
            String garbage,
            String projectName,
            String archive,
            String suffix
    ) {
        super(packageName, version, changeSet, garbage, projectName);
        this.archive = archive;
        this.suffix = suffix;
    }

    public String getArchive() {
        return archive;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OToolArchive)) return false;
        if (!super.equals(o)) return false;
        OToolArchive that = (OToolArchive) o;
        return Objects.equals(archive, that.archive) &&
                Objects.equals(suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), archive, suffix);
    }

    @Override
    public String toString() {
        return getPackageName() + '-' +
                getPackageName() + '-' +
                getRelease() + '.' +
                archive + suffix;
    }
}
