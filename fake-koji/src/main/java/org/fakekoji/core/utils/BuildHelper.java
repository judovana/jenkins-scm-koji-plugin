package org.fakekoji.core.utils;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.BuildProvider;
import hudson.plugins.scm.koji.model.RPM;
import org.fakekoji.functional.Result;
import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.model.OToolBuild;
import org.fakekoji.model.Platform;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.fakekoji.model.TaskVariantValue;
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
import java.util.Objects;
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

    private final File buildsRoot;
    private final GetBuildList params;
    private final OToolParser oToolParser;
    private final List<TaskVariant> buildVariants;
    private final Set<String> packageNames;
    private final TaskVariant buildPlatformVariant;
    private final BuildProvider buildProvider;

    private final Map<TaskVariant, String> buildVariantMap;
    private final String buildVariantString;

    private BuildHelper(
            final File buildsRoot,
            final GetBuildList params,
            final OToolParser oToolParser,
            final List<TaskVariant> buildVariants,
            final Set<String> packageNames,
            final TaskVariant buildPlatformVariant,
            final BuildProvider buildProvider
    ) {
        this.buildVariants = buildVariants;
        this.packageNames = packageNames;
        this.oToolParser = oToolParser;
        this.params = params;
        this.buildsRoot = buildsRoot;
        this.buildPlatformVariant = buildPlatformVariant;
        this.buildProvider = buildProvider;

        buildVariantMap = getBuildVariantMap();
        buildVariantString = getBuildVariantString();
    }

    private File getBuildRoot(OToolBuild build) {
        return Paths.get(
                buildsRoot.getAbsolutePath(),
                build.getPackageName(),
                build.getVersion(),
                build.getRelease()
        ).toFile();
    }

    private Comparator<File> getArchiveComparator() {
        return (archive1, archive2) -> Long.compare(archive2.lastModified(), archive1.lastModified());
    }

    private ArchiveState getArchiveState(final File root) {
        if (!root.exists()) {
            return ArchiveState.NOT_BUILT;
        }
        if (!root.isDirectory()) {
            LOGGER.warning(root.getAbsolutePath() + " is not a directory!");
            return ArchiveState.ERROR;
        }
        final File[] files = root.listFiles();
        if (files == null || files.length == 0) {
            return ArchiveState.NOT_BUILT;
        }
        if (files.length > 1) {
            LOGGER.warning("More than one file in " + root.getAbsolutePath());
            return ArchiveState.ERROR;
        }
        final File file = files[0];
        if (file.getName().equals("FAILED")) {
            LOGGER.info(file.getAbsolutePath() + " is failed");
            return ArchiveState.FAILED;
        }
        if (file.length() < 5) {
            LOGGER.info(file.getAbsolutePath() + " is less than 5 bytes");
            return ArchiveState.FAILED;
        }
        final Result<OToolArchive, String> result = oToolParser.parseArchive(file.getName());
        if (result.isOk()) {
            return ArchiveState.BUILT;
        }
        LOGGER.warning(result.getError());
        return ArchiveState.ERROR;
    }

    private Predicate<File> getArchiveRootPredicate(final ArchiveState state) {
        return root -> getArchiveState(root) == state;
    }

    public Function<OToolBuild, Optional<Build>> getBuildParser() {
        final List<String> platforms = Arrays.asList(params.getPlatforms().trim().split("\\p{javaWhitespace}+"));

        return build -> {
            final String packageName = build.getPackageName();
            final String version = build.getVersion();
            final String release = build.getRelease();
            final String nvr = packageName + '-' + version + '-' + release;

            // get directory of build: /name/version/release.project
            final File buildRoot = getBuildRoot(build);

            // get directories where required archives (src, dbg.jvm.os.arch)
            // are stored: /name/version/release.project/*
            final Supplier<Stream<File>> archiveRootStreamSupplier = () -> platforms.stream()
                    .map(archiveName -> new File(
                            buildRoot,
                            archiveName.equals(JenkinsJobTemplateBuilder.SOURCES) ? archiveName : buildVariantString + '.' + archiveName
                    ));

            // check if all needed archives are built
            if (!archiveRootStreamSupplier.get().allMatch(getArchiveRootPredicate(ArchiveState.BUILT))) {
                return Optional.empty();
            }

            final List<File> archiveFiles = archiveRootStreamSupplier.get()
                    .map(file -> Objects.requireNonNull(file.listFiles())[0])
                    .collect(Collectors.toList());

            final File newestArchive = archiveFiles.stream().min(getArchiveComparator()).orElse(buildRoot);

            final Function<File, RPM> toRPMs = archiveFile -> {
                final boolean isSource = archiveFile.getParentFile().getName().equals(JenkinsJobTemplateBuilder.SOURCES);
                final String[] parts = archiveFile.getName().split("\\.");
                final int length = parts.length;
                //final OToolArchive archive = oToolParser.parseArchive(archiveFile.getName());
                final String platform = isSource ? parts[length -2] : parts[length - 3] + '.' + parts[length - 2];
                return new RPM(
                        packageName,
                        version,
                        release,
                        isSource ? nvr : nvr + '.' + buildVariantString,
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
        final String buildPlatform = buildVariantMap.get(buildPlatformVariant);
        if (buildPlatform == null) {
            return build -> true;
        }

        final String requiredFilename = buildVariantString + '.' + buildPlatform;
        final ArchiveState archiveState = params.isBuilt() ? ArchiveState.BUILT : ArchiveState.NOT_BUILT;
        return build -> {
            final File archiveRoot = new File(getBuildRoot(build), requiredFilename);
            return getArchiveRootPredicate(archiveState).test(archiveRoot);
        };
    }

    public OToolParser getOToolParser() {
        return oToolParser;
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
        final List<TaskVariant> variants = new ArrayList<>(buildVariants);
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
            final ConfigManager configManager,
            GetBuildList params,
            File buildsRoot,
            BuildProvider buildProvider
    ) throws StorageException {

        final List<String> platforms = configManager.platformManager.readAll()
                .stream()
                .map(Platform::getId)
                .distinct()
                .collect(Collectors.toList());
        final List<TaskVariant> buildTaskVariants = configManager.taskVariantManager.getBuildVariants();

        final Set<String> packageNames = configManager.jdkVersionManager.readAll()
                .stream()
                .map(JDKVersion::getPackageNames)
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        final OToolParser parser = new OToolParser(
                configManager.jdkProjectManager.readAll(),
                configManager.jdkVersionManager.readAll(),
                buildTaskVariants
        );

        final TaskVariant buildPlatformVariant = new TaskVariant(
                BUILD_PLATFORM,
                BUILD_PLATFORM,
                Task.Type.BUILD,
                "",
                0,
                platforms.stream().collect(Collectors.toMap(id -> id, id -> new TaskVariantValue(id, id))),
                false
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

    enum ArchiveState {
        BUILT,
        ERROR,
        FAILED,
        NOT_BUILT,
    }
}
