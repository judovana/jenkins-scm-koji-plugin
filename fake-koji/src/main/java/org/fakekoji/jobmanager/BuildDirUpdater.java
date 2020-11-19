package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.utils.OToolParser;
import org.fakekoji.functional.Result;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class BuildDirUpdater {
    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);
    
    private final File buildsRoot;
    private final ConfigManager configManager;
    
    public BuildDirUpdater(final File buildsRoot, final ConfigManager configManager) {
        this.buildsRoot = buildsRoot;
        this.configManager = configManager;
    }
    
    public interface ArchiveTransformer {
        OToolArchive transform(final OToolArchive archive);
    }

    public void updateBuildDirs(final ArchiveTransformer transformer) {
        dirToFileStream(buildsRoot).forEach(packageDir -> dirToFileStream(packageDir).forEach(versionDir ->
                dirToFileStream(versionDir).forEach(buildDir -> dirToFileStream(buildDir).forEach(archiveDir ->
                        update(transformer, archiveDir)
                ))
        ));
    }

    private void update(final ArchiveTransformer transformer, final File archiveDir) {
        final File buildDir = archiveDir.getParentFile();
        final File versionDir = buildDir.getParentFile();
        final File packageDir = versionDir.getParentFile();
        final File logsDir = getLogsDir(buildDir);
        final String archiveName = OToolUtils.createArchiveName(packageDir, versionDir, buildDir, archiveDir);
        final File archive = new File(archiveDir, archiveName);
        if (!archive.exists()) {
            // if archive doesn't exist, then FAILED file should exist. If neither exists, something's wrong
            final File failed = new File(archiveDir, "FAILED");
            if (!failed.exists()) {
                LOGGER.warning("Dir " + archiveDir.getAbsolutePath() + " is empty");
            }
            return;
        }
        final Result<OToolArchive, String> parseResult = OToolParser.create(configManager)
                .flatMap(parser -> parser.parseArchive(archiveName));
        if (parseResult.isError()) {
            final String error = parseResult.getError();
            LOGGER.warning("Couldn't parse " + archiveName + ": " + error);
            return;
        }
        final OToolArchive oToolArchive = transformer.transform(parseResult.getValue());
        final File destFile = new File(archiveDir, oToolArchive.toNiceString());
        final File destDir = new File(buildDir, oToolArchive.getDirectoryName());
        Utils.moveDirSafe(archive, destFile);
        Utils.moveDirSafe(archiveDir, destDir);
        if (logsDir.exists()) {
            final File archiveLogsDir = new File(logsDir, archiveDir.getName());
            if (!archiveLogsDir.exists()) {
                return;
            }
            final File destLogDir = new File(logsDir, oToolArchive.getDirectoryName());
            try {
                Files.move(archiveLogsDir.toPath(), destLogDir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Stream<File> dirToFileStream(final File dir) {
        return Arrays.stream(Objects.requireNonNull(dir.listFiles()));
    }

    private File getLogsDir(final File buildDir) {
        return Paths.get(buildDir.getAbsolutePath(), "data", "logs").toFile();
    }
}
