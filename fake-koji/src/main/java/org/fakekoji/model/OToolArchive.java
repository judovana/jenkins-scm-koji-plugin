package org.fakekoji.model;

import org.fakekoji.functional.Tuple;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OToolArchive extends OToolBuild {

    private final List<Tuple<String, String>> buildVariants;
    private final String platform;
    private final String suffix;

    public OToolArchive(
            String packageName,
            String version,
            String changeSet,
            String garbage,
            String projectName,
            List<Tuple<String, String>> buildVariants,
            String platform,
            String suffix
    ) {
        super(packageName, version, changeSet, garbage, projectName);
        this.buildVariants = buildVariants;
        this.platform = platform;
        this.suffix = suffix;
    }

    public OToolArchive(
            OToolBuild oToolBuild,
            List<Tuple<String, String>> buildVariants,
            String platform,
            String suffix
    ) {
        this(
                oToolBuild.getPackageName(),
                oToolBuild.getVersion(),
                oToolBuild.getChangeSet(),
                oToolBuild.getGarbage(),
                oToolBuild.getProjectName(),
                buildVariants,
                platform,
                suffix
        );

    }

    public List<Tuple<String, String>> getBuildVariants() {
        return buildVariants;
    }

    public String getPlatform() {
        return platform;
    }

    public String getSuffix() {
        return suffix;
    }

    public String joinBuildVariants() {
        return buildVariants.stream().map(variant -> variant.y).collect(Collectors.joining("."));
    }

    public String getDirectoryName() {
        if (buildVariants.isEmpty()) {
            return platform;
        }
        return joinBuildVariants() + '.' + platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OToolArchive)) return false;
        if (!super.equals(o)) return false;
        OToolArchive that = (OToolArchive) o;
        return Objects.equals(buildVariants, that.buildVariants) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(suffix, that.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), buildVariants, platform, suffix);
    }

    @Override
    public String toString() {
        return "OToolArchive{" +
                "buildVariants=" + buildVariants +
                ", platform='" + platform + '\'' +
                ", suffix='" + suffix + '\'' +
                "} " + super.toString();
    }
}
