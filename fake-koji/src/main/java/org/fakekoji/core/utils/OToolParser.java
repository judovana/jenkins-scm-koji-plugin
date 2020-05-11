package org.fakekoji.core.utils;

import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.model.OToolBuild;
import org.fakekoji.model.TaskVariant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.fakekoji.jobmanager.JenkinsJobTemplateBuilder.SOURCES;

public class OToolParser {

    static final String WRONG_FORMAT = "Wrong format: ";
    static final String UNKNOWN_PACKAGE_NAME_ERROR = "unknown package name";
    static final String DASH_SPLIT_ERROR = WRONG_FORMAT + "not marching N-V-R";
    static final String UNKNOWN_PROJECT_NAME_ERROR = "unknown project name: ";
    static final String CHANGE_SET_OR_PROJECT_NAME_MISSING_ERROR = WRONG_FORMAT + "change set or project name missing";

    private final List<JDKProject> jdkProjects;
    private final List<JDKVersion> jdkVersions;
    private final List<TaskVariant> buildVariants;

    public OToolParser(
            List<JDKProject> jdkProjects,
            List<JDKVersion> jdkVersions,
            List<TaskVariant> buildVariants
    ) {
        this.jdkVersions = jdkVersions;
        this.jdkProjects = jdkProjects;
        this.buildVariants = buildVariants.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
    }

    private Result<PackageNameCut, String> parsePackageName(final String nvr) {
        return jdkVersions.stream()
                .map(JDKVersion::getPackageNames)
                .flatMap(Collection::stream)
                .filter(packageName -> nvr.startsWith(packageName + '-'))
                .min((a, b) -> b.length() - a.length())
                .<Result<PackageNameCut, String>>map(packageName ->
                        Result.ok(new PackageNameCut(packageName, nvr.replace(packageName + '-', "")))
                )
                .orElseGet(() -> Result.err(UNKNOWN_PACKAGE_NAME_ERROR));
    }

    private Result<ChangeSetCut, String> parseChangeSet(final PackageNameCut packageNameCut) {
        final String packageName = packageNameCut.packageName;
        final String[] tailSplit = packageNameCut.tail.split("-");
        if (tailSplit.length != 2) {
            return Result.err(DASH_SPLIT_ERROR);
        }
        final String version = tailSplit[0];
        final String release = tailSplit[1];
        final String[] releaseSplit = release.split("\\.");
        if (releaseSplit.length < 2) {
            return Result.err(CHANGE_SET_OR_PROJECT_NAME_MISSING_ERROR);
        }
        final String changeSet = releaseSplit[0];
        return Result.ok(new ChangeSetCut(
                packageName,
                version,
                changeSet,
                String.join(".", Arrays.copyOfRange(releaseSplit, 1, releaseSplit.length))
        ));
    }

    public Result<OToolArchive, String> parseArchive(final String nvra) {
        return parsePackageName(nvra)
                .flatMap(this::parseChangeSet)
                .flatMap(this::parseArchiveTailCut)
                .flatMap(archiveTailCut -> Result.ok(new OToolArchive(
                        archiveTailCut.packageName,
                        archiveTailCut.version,
                        archiveTailCut.changeSet,
                        archiveTailCut.garbage,
                        archiveTailCut.projectName,
                        archiveTailCut.buildVariants,
                        archiveTailCut.platform,
                        archiveTailCut.suffix
                )));
    }

    public Result<OToolBuild, String> parseBuild(final String nvr) {
        return parsePackageName(nvr)
                .flatMap(this::parseChangeSet)
                .flatMap(this::parseBuildTailCut)
                .flatMap(buildTailCut -> Result.ok(new OToolBuild(
                        buildTailCut.packageName,
                        buildTailCut.version,
                        buildTailCut.changeSet,
                        buildTailCut.garbage,
                        buildTailCut.projectName
                )));
    }

    private Result<BuildTailCut, String> parseBuildTailCut(final ChangeSetCut changeSetCut) {
        final String[] tailParts = changeSetCut.tail.split("\\.");
        final int projectIndex = tailParts.length - 1;
        final Optional<String> projectNameOpt = jdkProjects.stream()
                .map(Project::getId)
                .filter(name -> name.equals(tailParts[projectIndex]))
                .findFirst();
        return projectNameOpt.<Result<BuildTailCut, String>>map(projectName -> {
            final String garbage = Arrays.stream(tailParts).limit(projectIndex).collect(Collectors.joining("."));
            return Result.ok(new BuildTailCut(changeSetCut, garbage, projectName));
        })
                .orElse(Result.err(UNKNOWN_PROJECT_NAME_ERROR + tailParts[projectIndex]));
    }

    private Result<ArchiveTailCut, String> parseArchiveTailCut(final ChangeSetCut changeSetCut) {
        final String[] tailParts = changeSetCut.tail.split("\\.");
        final int length = tailParts.length;
        final String suffix = tailParts[length - 1];
        final String platform;
        final List<Tuple<String, String>> variants;
        final int tailEnd;
        if (tailParts[length - 2].equals(SOURCES)) {
            variants = Collections.emptyList();
            platform = tailParts[length - 2];
            tailEnd = length - 2;
        } else {
            platform = tailParts[length - 3] + '.' + tailParts[length - 2];
            int i = -1;
            variants = new ArrayList<>();
            final int lastBuildVariantIndex = length - 4;
            for (final TaskVariant buildVariant : buildVariants) {
                final int index = lastBuildVariantIndex - (i + 1);
                final String value;
                if (index < 1) {
                    return Result.err(CHANGE_SET_OR_PROJECT_NAME_MISSING_ERROR);
                }
                if (buildVariant.getVariants().containsKey(tailParts[index])) {
                    value = tailParts[index];
                    i++;
                } else {
                    value = buildVariant.getDefaultValue();
                }
                variants.add(0, new Tuple<>(buildVariant.getId(), value));
            }
            tailEnd = lastBuildVariantIndex - i;
        }
        return parseBuildTailCut(new ChangeSetCut(
                changeSetCut.packageName,
                changeSetCut.version,
                changeSetCut.changeSet,
                String.join(".", Arrays.copyOfRange(tailParts, 0, tailEnd))
        )).flatMap(buildTailCut -> Result.ok(new ArchiveTailCut(
                buildTailCut,
                variants,
                platform,
                suffix
        )));
    }

    private static class PackageNameCut {
        final String packageName;
        final String tail;

        PackageNameCut(String packageName, String tail) {
            this.packageName = packageName;
            this.tail = tail;
        }
    }

    private static class ChangeSetCut extends PackageNameCut {
        final String version;
        final String changeSet;

        ChangeSetCut(String packageName, String version, String changeSet, String tail) {
            super(packageName, tail);
            this.version = version;
            this.changeSet = changeSet;
        }

        ChangeSetCut(ChangeSetCut changeSetCut) {
            this(changeSetCut.packageName, changeSetCut.version, changeSetCut.changeSet, changeSetCut.tail);
        }
    }

    private static class BuildTailCut extends ChangeSetCut {
        protected final String garbage;
        protected final String projectName;

        BuildTailCut(ChangeSetCut changeSetCut, String garbage, String projectName) {
            super(changeSetCut);
            this.garbage = garbage;
            this.projectName = projectName;
        }
    }

    private static class ArchiveTailCut extends BuildTailCut {
        final String suffix;
        final String platform;
        final List<Tuple<String, String>> buildVariants;

        ArchiveTailCut(
                BuildTailCut buildTailCut,
                List<Tuple<String, String>> buildVariants,
                String platform,
                String suffix
        ) {
            super(buildTailCut, buildTailCut.garbage, buildTailCut.projectName);
            this.suffix = suffix;
            this.platform = platform;
            this.buildVariants = buildVariants;
        }
    }

    public static class LegacyNVR {
        private final String n;
        private final String v;
        private final String r;
        private final String nvr;

        public LegacyNVR(String nvr) {
            String nv = nvr.substring(0, nvr.lastIndexOf("-"));
            String n = nv.substring(0, nv.lastIndexOf("-"));
            String[] vr = nvr.replaceFirst(n + "-", "").split("-");
            String[] splitted = new String[]{n, vr[0], vr[1]};
            this.nvr = nvr;
            this.n = n;
            this.v = vr[0];
            this.r = vr[1];
        }

        public String getN() {
            return n;
        }

        public String getV() {
            return v;
        }

        public String getR() {
            return r;
        }

        public String getNV() {
            return n + "-" + v;
        }

        public String getVR() {
            return v + "-" + r;
        }

        public String getNR() {
            return n + "-" + r;
        }

        public String getNVR() {
            return nvr;
        }
        public String getPartialPath() {
            return n+"/"+v;
        }

        public String getFullPath() {
            return getPartialPath()+"/"+r;
        }
    }

    public static class LegacyNVRA extends LegacyNVR {
        private final String arch;
        private final String os;
        private final String nvra;

        public LegacyNVRA(String nvra) {
            super(removeOsArch(nvra));
            this.nvra = nvra;
            String oa = nvra.replace(this.getNVR() + ".", "");
            String[] ooaa = oa.split("\\.");
            os = ooaa[0];
            arch = ooaa[1];
        }

        private static String removeOsArch(String nvroa) {
            String nvro = nvroa.substring(0, nvroa.lastIndexOf("."));
            String nvr = nvro.substring(0, nvro.lastIndexOf("."));
            return nvr;
        }

        public String getA() {
            return os + "." + arch;
        }

        public String getFullR() {
            return getR()+"."+os;
        }

        public String getArch() {
            return arch;
        }

        public String getOs() {
            return os;
        }

        public String getNVRA() {
            return nvra;
        }

        @Override
        public String getFullPath() {
            return getPartialPath()+"/"+getFullR()+"/"+arch;
        }

    }

    public static class LegacyNVRASuffix extends LegacyNVRA {
        private final String nvras;
        private final String suffix;

        public LegacyNVRASuffix(String nvras) {
            super(nvras.substring(0, nvras.lastIndexOf(".")));
            String suffix = nvras.replace(this.getNVRA() + ".", "");
            this.nvras = nvras;
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getNvras() {
            return nvras;
        }
    }
}
