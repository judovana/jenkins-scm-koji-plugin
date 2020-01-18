package org.fakekoji.core.utils;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;
import hudson.plugins.scm.koji.model.RPM;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolBuild;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildList;

import java.io.File;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BuildHelper {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private static final String BUILD_PLATFORM = "buildPlatform";
    private static final String SOURCES = "src";

    private final File buildsRoot;
    private final GetBuildList params;
    private final OToolBuildParser oToolBuildParser;
    private final List<TaskVariant> taskVariants;
    private final Set<String> packageNames;
    private final TaskVariant buildPlatformVariant;
    private final BuildProvider buildProvider;

    private final Map<TaskVariant, String> buildVariantMap;
    private final String buildVariantString;

    private BuildHelper(
            final File buildsRoot,
            final GetBuildList params,
            final OToolBuildParser oToolBuildParser,
            final List<TaskVariant> taskVariants,
            final Set<String> packageNames,
            final TaskVariant buildPlatformVariant,
            final BuildProvider buildProvider
    ) {
        this.taskVariants = taskVariants;
        this.packageNames = packageNames;
        this.oToolBuildParser = oToolBuildParser;
        this.params = params;
        this.buildsRoot = buildsRoot;
        this.buildPlatformVariant = buildPlatformVariant;
        this.buildProvider = buildProvider;

        buildVariantMap = getBuildVariantMap();
        buildVariantString = getBuildVariantString();
    }


    private Comparator<File> getArchiveComparator() {
        return (archive1, archive2) -> Long.compare(archive2.lastModified(), archive1.lastModified());
    }

    public Function<String, Optional<OToolBuild>> getOToolBuildParser() {
        return nvr -> {
            try {
                return Optional.of(oToolBuildParser.parse(nvr));
            } catch (ParserException e) {
                return Optional.empty();
            }
        };
    }

    public Function<OToolBuild, Optional<Build>> getBuildParser() {
        final List<String> platforms = Arrays.asList(params.getPlatforms().trim().split("\\p{javaWhitespace}+"));

        return build -> {
            final String packageName = build.getPackageName();
            final String version = build.getVersion();
            final String release = build.getRelease();
            final String nvr = packageName + '-' + version + '-' + release;

            // get directory of build: /name/version/release.project
            final File buildRoot = Paths.get(buildsRoot.getAbsolutePath(), packageName, version, release).toFile();

            // get directories where required archives (src, dbg.jvm.os.arch)
            // are stored: /name/version/release.project/*
            final Supplier<Stream<File>> archiveRootStreamSupplier = () -> platforms.stream()
                    .map(archiveName -> new File(
                            buildRoot,
                            archiveName.equals(SOURCES) ? archiveName : buildVariantString + '.' + archiveName
                    ));

            // check whether root exists and is a directory
            final Predicate<File> archiveRootFilter = root -> {
                if (!root.exists()) {
                    return false;
                }
                if (!root.isDirectory()) {
                    LOGGER.warning(root.getAbsolutePath() + " is not a directory!");
                    return false;
                }
                return true;
            };

            // if one or more archive directories does not exist or is file (which should never happen),
            // discard build
            if (!archiveRootStreamSupplier.get().allMatch(archiveRootFilter)) {
                return Optional.empty();
            }

            final List<File> archiveRoots = archiveRootStreamSupplier.get().collect(Collectors.toList());

            // get archive from archive root:
            // /name/version/release.project/dbg.jvm.os.arch/name-version-release.project.dbg.jvm.os.arch.suffix
            final Supplier<Stream<File>> archiveFileStreamSupplier = () -> archiveRoots.stream()
                    .map(root -> new File(root, nvr + '.' + root.getName() + ".tarxz"));

            // check whether archive exists and is not a directory
            final Predicate<File> archiveFileFilter = file -> {
                    if (!file.exists()) {
                        return false;
                    }
                    if (file.isDirectory()) {
                        LOGGER.warning(file.getAbsolutePath() + " is a directory!");
                        return false;
                    }
                    return true;
            };

            // discard build if one or more archives don't exist or are directories
            if (!archiveFileStreamSupplier.get().allMatch(archiveFileFilter)) {
                return Optional.empty();
            }

            final List<File> archiveFiles = archiveFileStreamSupplier.get().collect(Collectors.toList());

            final File newestArchive = archiveFiles.stream().min(getArchiveComparator()).orElse(buildRoot);

            final Function<File, RPM> toRPMs = archiveFile -> {
                final String[] parts = archiveFile.getName().split("\\.");
                final int length = parts.length;
                final String platform = parts[length - 3] + '.' + parts[length - 2];
                return new RPM(
                        packageName,
                        version,
                        release,
                        nvr,
                        platform,
                        archiveFile.getName(),
                        String.join("/",
                                "http://" + buildProvider.getDownloadUrl(),
                                packageName,
                                version,
                                release,
                                archiveFile.getParentFile().getName(),
                                archiveFile.getName()
                        )
                );
            };

            final List<RPM> rpms = archiveFiles.stream().map(toRPMs).collect(Collectors.toList());
            final String completionTime = Constants.DTF
                    .format(
                            new Date(newestArchive.lastModified())
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                    );

            return Optional.of(new Build(
                    buildRoot.hashCode(),
                    packageName,
                    version,
                    release,
                    nvr,
                    completionTime,
                    rpms,
                    Collections.emptySet(),
                    buildProvider,
                    null
            ));
        };
    }

    public Predicate<OToolBuild> getPackageNamePredicate() {
        return build -> packageNames.contains(build.getPackageName());
    }

    public Predicate<OToolBuild> getProjectNamePredicate() {
        return build -> params.getProjectName().equals(build.getProjectName());
    }

    public Predicate<OToolBuild> getBuildPlatformPredicate() {
        // if params contains buildPlatform variant in buildVariants field, check whether archive of that platform
        // exists or not depending on params' isBuilt field
        return build -> {
            final Optional<String> buildPlatformOptional = Optional.ofNullable(
                    buildVariantMap.get(buildPlatformVariant)
            );
            if (buildPlatformOptional.isPresent()) {
                final String requiredFilename = buildVariantString + '.' + buildPlatformOptional.get();
                final File archiveRoot = Paths.get(
                        buildsRoot.getAbsolutePath(),
                        build.getPackageName(),
                        build.getVersion(),
                        build.getRelease(),
                        requiredFilename
                ).toFile();
                return archiveRoot.exists() == params.isBuilt();
            }
            return true;
        };
    }

    private String getBuildVariantString() {
        return buildVariantMap
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().getId().equals(BuildHelper.BUILD_PLATFORM))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.joining("."));
    }

    private Map<TaskVariant, String> getBuildVariantMap() {
        final Map<TaskVariant, String> buildVariantMap = new HashMap<>();
        final List<TaskVariant> variants = new ArrayList<>(taskVariants);
        variants.add(buildPlatformVariant);
        for (final TaskVariant taskVariant : variants) {
            final Optional<String> value = getBuildVariantValue(taskVariant.getId());
            value.ifPresent(s -> buildVariantMap.put(taskVariant, s));
        }
        return buildVariantMap;
    }

    private Optional<String> getBuildVariantValue(String taskVariantId) {
        final Matcher matcher = getBuildVariantPattern(taskVariantId).matcher(params.getBuildVariants());
        if (matcher.find()) {
            return Optional.of(matcher.group(3));
        }
        return Optional.empty();
    }

    private static Pattern getBuildVariantPattern(String buildVariant) {
        return Pattern.compile("(\\s|^)(" + buildVariant + "=)(.*?)(\\s|$)");
    }

    public static BuildHelper create(
            ConfigManager configManager,
            GetBuildList params,
            File buildsRoot,
            BuildProvider buildProvider
    ) throws StorageException {
        final Storage<TaskVariant> taskVariantStorage = configManager.getTaskVariantStorage();
        final Storage<Platform> platformStorage = configManager.getPlatformStorage();
        final Storage<JDKVersion> jdkVersionStorage = configManager.getJdkVersionStorage();
        final Storage<JDKProject> jdkProjectStorage = configManager.getJdkProjectStorage();

        final List<String> platforms = platformStorage.loadAll(Platform.class)
                .stream()
                .map(Platform::assembleString)
                .distinct()
                .collect(Collectors.toList());
        final List<TaskVariant> buildTaskVariants = taskVariantStorage.loadAll(TaskVariant.class)
                .stream()
                .filter(taskVariant -> taskVariant.getType() == Task.Type.BUILD)
                .collect(Collectors.toList());

        final Set<String> packageNames = jdkVersionStorage.loadAll(JDKVersion.class)
                .stream()
                .map(JDKVersion::getPackageNames)
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        final OToolBuildParser parser = new OToolBuildParser(
                jdkVersionStorage.loadAll(JDKVersion.class),
                jdkProjectStorage.loadAll(JDKProject.class)
        );

        final TaskVariant buildPlatformVariant = new TaskVariant(
                BUILD_PLATFORM,
                BUILD_PLATFORM,
                Task.Type.BUILD,
                "",
                0,
                platforms.stream().collect(Collectors.toMap(id -> id, id -> new TaskVariantValue(id, id)))
        );

        return new BuildHelper(
                buildsRoot,
                params,
                parser,
                buildTaskVariants,
                packageNames,
                buildPlatformVariant,
                buildProvider
        );
    }
}
