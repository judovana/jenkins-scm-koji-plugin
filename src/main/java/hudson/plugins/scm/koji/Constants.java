package hudson.plugins.scm.koji;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

abstract public class Constants {

    public static final String BUILD_XML = "build.xml";
    public static final String BUILD_ENV_NVR = "KOJI_NVR";
    public static final String BUILD_ENV_RPM_FILES = "KOJI_RPMS";
    public static final String BUILD_ENV_RPMS_DIR = "KOJI_RPMS_DIR";
    public static final String PROCESSED_BUILDS_HISTORY = "processed.txt";
    public static final String getPackageID = "getPackageID";
    public static final String listBuilds = "listBuilds";
    public static final String packageID = "packageID";
    public static final String listTags = "listTags";
    public static final String listRPMs = "listRPMs";
    public static final String listArchives = "listArchives";
    public static final String buildID = "buildID";
    public static final String arches = "arches";
    public static final String build = "build";
    public static final String name = "name";
    public static final String rpms = "rpms";
    public static final String nvr = "nvr";
    public static final String completion_time = "completion_time";
    public static final String build_id = "build_id";
    public static final String version = "version";
    public static final String release = "release";
    public static final String arch = "arch";
    public static final String filename = "filename";

    public static final DateTimeFormatter DTF = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.MICRO_OF_SECOND)
            .toFormatter();


    /**
     * 2016-09-09T13:34:28
     */
 public static final DateTimeFormatter DTF2 = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter();

}
