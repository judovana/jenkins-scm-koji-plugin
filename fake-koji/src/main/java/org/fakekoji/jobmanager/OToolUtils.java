package org.fakekoji.jobmanager;

import java.io.File;

public class OToolUtils {

    public static String createArchiveName(
            final File packageDir,
            final File versionDir,
            final File buildDir,
            final File archiveDir
    ) {
        return createArchiveName(packageDir.getName(), versionDir.getName(), buildDir.getName(), archiveDir.getName());
    }

    public static String createArchiveName(
            final String packageName,
            final String version,
            final String build,
            final String archive
    ) {
        return String.join(
                "-",
                packageName,
                version,
                build + "." + archive + ".tarxz"
        );
    }
}
