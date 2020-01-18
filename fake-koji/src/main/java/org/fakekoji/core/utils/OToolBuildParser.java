package org.fakekoji.core.utils;

import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolBuild;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OToolBuildParser {

    private static final int NVR_MIN_LENGTH = 3;
    private static final int RELEASE_MIN_LENGTH = 2;
    static final String ERROR_NVR_SPLIT_LENGTH = "'-' split of NVR is less than " + NVR_MIN_LENGTH + " parts long";
    static final String ERROR_RELEASE_SPLIT_LENGTH = "'.' split of release is less than " +RELEASE_MIN_LENGTH + " parts long";

    private final List<JDKVersion> jdkVersions;
    private final List<JDKProject> jdkProjects;

    OToolBuildParser(
            List<JDKVersion> jdkVersions,
            List<JDKProject> jdkProjects
    ) {
        this.jdkVersions = jdkVersions;
        this.jdkProjects = jdkProjects;
    }

    public OToolBuild parse(final String nvr) throws ParserException {
        final String[] nvrSplit = nvr.split("-");
        final int nvrLength = nvrSplit.length;
        if (nvrLength < NVR_MIN_LENGTH) {
            throw new ParserException(ERROR_NVR_SPLIT_LENGTH);
        }

        final String version = nvrSplit[nvrLength - (NVR_MIN_LENGTH - 1)];
        final String packageName = Arrays.stream(nvrSplit).limit(nvrLength - 2).collect(Collectors.joining("-"));
        final String[] releaseSplit = nvrSplit[nvrLength - 1].split("\\.");

        // release contains change set, some additional info (chaos), project name, debug mode and jvm separated by dot
        // change set and project name are required, so if length is less than 2, something's wrong
        final int releaseLength = releaseSplit.length;
        if (releaseLength < RELEASE_MIN_LENGTH) {
            throw new ParserException(ERROR_RELEASE_SPLIT_LENGTH);
        }
        final String changeSet = releaseSplit[0];
        final String projectName = releaseSplit[releaseLength - (RELEASE_MIN_LENGTH - 1)];

        final String chaos;
        // if length is greater than 4, there is some garbage
        if (releaseLength > RELEASE_MIN_LENGTH) {
            chaos = String.join(".", Arrays.copyOfRange(releaseSplit, 1, releaseLength - 1));
        } else {
            chaos = "";
        }

        if (jdkVersions.stream().noneMatch(jdkVersion -> jdkVersion.getPackageNames().contains(packageName))) {
            throw new ParserException("Unknown package name: " + packageName);
        }
        if (jdkProjects.stream().noneMatch(jdkProject -> jdkProject.getId().equals(projectName))) {
            throw new ParserException("Unknown project name: " + projectName);
        }
        return new OToolBuild(
                packageName,
                version,
                changeSet,
                chaos,
                projectName
        );
    }
}
