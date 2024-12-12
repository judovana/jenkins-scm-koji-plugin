package org.fakekoji.jobmanager;

import org.fakekoji.Utils;
import org.fakekoji.core.utils.OToolParser;
import org.fakekoji.functional.Result;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class BuildDirUpdater {
    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private final File buildsRoot;
    private final ConfigManager configManager;
    private final List<String> errorLogs = new ArrayList<>();
    private long total = 0;
    private long failed = 0;
    private long skipped = 0;

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
        total++;
        if (!archive.exists()) {
            skipped++;
            return;
        }
        final Result<OToolArchive, String> parseResult = OToolParser.create(configManager)
                .flatMap(parser -> parser.parseArchive(archiveName));
        if (parseResult.isError()) {
            final String error = parseResult.getError();
            final String message = "Couldn't parse " + archiveName + ": " + error;
            LOGGER.warning(message);
            addErrorLog(archiveName, message);
            failed++;
            return;
        }
        final OToolArchive oToolArchive = transformer.transform(parseResult.getValue());
        final String newArchiveName = oToolArchive.toNiceString();
        final File destFile = new File(archiveDir, newArchiveName);
        final File destDir = new File(buildDir, oToolArchive.getDirectoryName());
        final Result<Void, String> moveResult = moveArchive(archive, destFile).flatMap(result1 ->
                moveArchive(archiveDir, destDir).flatMap(result2 -> {
                    if (logsDir.exists()) {
                        final File archiveLogsDir = new File(logsDir, archiveDir.getName());
                        if (!archiveLogsDir.exists()) {
                            return Result.ok(null);
                        }
                        final File destLogDir = new File(logsDir, oToolArchive.getDirectoryName());
                        return moveArchive(archiveLogsDir, destLogDir);
                    } else {
                        return Result.ok(null);
                    }
                })
        );
        if (moveResult.isError()) {
            failed++;
            final String error = moveResult.getError();
            addErrorLog(newArchiveName, error);
        }
    }

    private void addErrorLog(final String archiveName, final String message) {
        errorLogs.add(archiveName + ": " + message);
    }

    public Result<Void, String> moveArchive(final File src, final File dst) {
        final String srcAbs = src.getAbsolutePath();
        final String dstAbs = dst.getAbsolutePath();
        try {
            LOGGER.info("Moving " + srcAbs + " to " + dstAbs);
            Utils.moveDirByMvDefault(src, dst);
        } catch (IOException e) {
            final String message = "Failed to move " + srcAbs + " to " + dstAbs + ": " + e.getMessage();
            LOGGER.log(Level.SEVERE, message, e);
        }
        return Result.ok(null);
    }

    private Stream<File> dirToFileStream(final File dir) {
        return Arrays.stream(Objects.requireNonNull(dir.listFiles()));
    }

    private File getLogsDir(final File buildDir) {
        return Paths.get(buildDir.getAbsolutePath(), "data", "logs").toFile();
    }

    public BuildUpdateSummary getSummary() {
        return new BuildUpdateSummary(total, failed, skipped, errorLogs);
    }

    public static class BuildUpdateSummary {
        private final List<String> errorLogs;
        private final long total;
        private final long failed;
        private final long skipped;

        public BuildUpdateSummary(
                final long total,
                final long failed,
                final long skipped,
                final List<String> errorLogs
        ) {
            this.total = total;
            this.failed = failed;
            this.skipped = skipped;
            this.errorLogs = errorLogs;
        }

        public long getTotal() {
            return total;
        }

        public long getFailed() {
            return failed;
        }

        public long getSkipped() {
            return skipped;
        }

        public List<String> getErrorLogs() {
            return errorLogs;
        }
    }
}
