package org.fakekoji.core.utils;

import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.model.OToolArchive;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OToolArchiveParser {

    private static final int NVR_MIN_LENGTH = 3;

    private final List<JDKProject> jdkProjects;

    public OToolArchiveParser(List<JDKProject> jdkProjects) {
        this.jdkProjects = jdkProjects;
    }

    public Optional<OToolArchive> parse(final String nvra) {

        final String[] nvrSplit = nvra.split("-");
        final int nvrLength = nvrSplit.length;
        if (nvrLength < NVR_MIN_LENGTH) {
            return Optional.empty();
        }

        final String version = nvrSplit[nvrLength - (NVR_MIN_LENGTH - 1)];
        final String packageName = Arrays.stream(nvrSplit).limit(nvrLength - 2).collect(Collectors.joining("-"));
        final String[] releaseSplit = nvrSplit[nvrLength - 1].split("\\.");
        final int releaseLength = releaseSplit.length;
        // release contains change set, some additional info (chaos), project name, debug mode and jvm separated by dot
        // change set and project name are required, so if length is less than 2, something's wrong
        final String changeSet;
        final String chaos;
        final String projectName;
        final String archive;
        final String suffix;
        try {
            suffix = releaseSplit[releaseLength - 1];
            changeSet = releaseSplit[0];
            // begin to search for the project from the end, skip suffix and platform (can be of length 1 or 2)
            final int archiveEnd = releaseLength - (releaseSplit[releaseLength - 2].equals(JenkinsJobTemplateBuilder.SOURCES) ? 3 : 4);
            for (int i = archiveEnd; i > 0; i--) {
                final String releasePart = releaseSplit[i];
                if (jdkProjects.stream().anyMatch(jdkProject -> jdkProject.getId().equals(releasePart))) {
                    // archive is what lies between project and suffix (release.hotspot.f28.x86_64
                    archive = String.join(".", Arrays.copyOfRange(
                            releaseSplit,
                            i + 1,
                            releaseLength - 1
                    ));
                    // if project doesn't follow right after change set, there is some chaos
                    chaos = i > 1 ? String.join(".", Arrays.copyOfRange(releaseSplit, 1, i)) : "";
                    projectName = releasePart;
                    return Optional.of(new OToolArchive(
                            packageName,
                            version,
                            changeSet,
                            chaos,
                            projectName,
                            archive,
                            suffix
                    ));
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        return Optional.empty();
    }
}
