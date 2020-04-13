package org.fakekoji.core.utils;

import org.fakekoji.DataGenerator;
import org.fakekoji.functional.Result;
import org.fakekoji.functional.Tuple;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolArchive;
import org.fakekoji.model.OToolBuild;
import org.fakekoji.model.Task;
import org.fakekoji.model.TaskVariant;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.fakekoji.DataGenerator.DEBUG_MODE;
import static org.fakekoji.DataGenerator.FASTDEBUG;
import static org.fakekoji.DataGenerator.F_29_X64;
import static org.fakekoji.DataGenerator.HOTSPOT;
import static org.fakekoji.DataGenerator.JDK_8_PACKAGE_NAME;
import static org.fakekoji.DataGenerator.JVM;
import static org.fakekoji.DataGenerator.PROJECT_NAME_U;
import static org.fakekoji.DataGenerator.RELEASE;

public class OToolParserTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private static final List<JDKVersion> jdkVersions = new ArrayList<>(DataGenerator.getJDKVersions());

    private static final List<JDKProject> jdkProjects = new ArrayList<>(DataGenerator.getJDKProjects());

    private static final List<TaskVariant> buildVariants = DataGenerator.getTaskVariants()
            .stream()
            .filter(taskVariant -> taskVariant.getType() == Task.Type.BUILD)
            .sorted(TaskVariant::compareTo)
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());


    private static OToolParser parser = new OToolParser(
            jdkProjects, jdkVersions, buildVariants
    );

    private static final String VERSION = "version";
    private static final String CHANGE_SET = "152";
    private static final String CHAOS = "CH.AO.S";
    private static final String INVALID_PACKAGE_NAME = "invalidPckgName";
    private static final String SUFFIX = "tgz";

    private static final String VALID_NAME_VERSION = String.join("-", JDK_8_PACKAGE_NAME, VERSION);
    private static final String VALID_RELEASE = String.join(".", CHANGE_SET, PROJECT_NAME_U);
    private static final String VALID_RELEASE_WITH_CHAOS = String.join(".", CHANGE_SET, CHAOS, PROJECT_NAME_U);
    private static final String VALID_ARCHIVE_FULL = String.join(".", RELEASE, HOTSPOT,F_29_X64, SUFFIX);
    private static final String VALID_ARCHIVE_MISSING = String.join(".", FASTDEBUG, F_29_X64, SUFFIX);

    @Test
    public void parseValidNVRWithNoGarbage() {
        final String nvr = VALID_NAME_VERSION + "-" + VALID_RELEASE;
        final Result<OToolBuild, String> result = parser.parseBuild(nvr);
        Assert.assertEquals(
                new OToolBuild(
                        JDK_8_PACKAGE_NAME,
                        VERSION,
                        CHANGE_SET,
                        "",
                        PROJECT_NAME_U
                ),
                result.getValue()
        );
    }

    @Test
    public void parseValidNVRWithGarbage() {

        final String nvr = VALID_NAME_VERSION + "-" + VALID_RELEASE_WITH_CHAOS;
        final Result<OToolBuild, String> result = parser.parseBuild(nvr);
        Assert.assertEquals(
                new OToolBuild(
                        JDK_8_PACKAGE_NAME,
                        VERSION,
                        CHANGE_SET,
                        CHAOS,
                        PROJECT_NAME_U
                ),
                result.getValue()
        );
    }

    @Test
    public void parseNVRWithInvalidPackageName() {
        final String nvr = INVALID_PACKAGE_NAME + "-" + VERSION + "-" + VALID_RELEASE;
        final Result<OToolBuild, String> result = parser.parseBuild(nvr);
        Assert.assertEquals(
                OToolParser.UNKNOWN_PACKAGE_NAME_ERROR,
                result.getError()
        );
    }

    @Test
    public void parseNVRWithMissingPackageName() {
        final String nvr = VERSION + "-" + VALID_RELEASE;
        final Result<OToolBuild, String> result = parser.parseBuild(nvr);
        Assert.assertEquals(
                OToolParser.UNKNOWN_PACKAGE_NAME_ERROR,
                result.getError()
        );
    }

    @Test
    public void parseNVRWithMissingVersion() {
        final String nvr = JDK_8_PACKAGE_NAME + "-" + VALID_RELEASE;
        final Result<OToolBuild, String> result = parser.parseBuild(nvr);
        Assert.assertEquals(
                OToolParser.DASH_SPLIT_ERROR,
                result.getError()
        );
    }

    @Test
    public void parseNVRWithInvalidRelease() {
        final String nvr = VALID_NAME_VERSION + "-" + PROJECT_NAME_U;
        final Result<OToolBuild, String> result = parser.parseBuild(nvr);
        Assert.assertEquals(
                OToolParser.CHANGE_SET_OR_PROJECT_NAME_MISSING_ERROR,
                result.getError()
        );
    }

    @Test
    public void parseValidNVRAWithoutGarbage() {
        final String nvra = VALID_NAME_VERSION + "-" + VALID_RELEASE + "." + VALID_ARCHIVE_FULL;
        final Result<OToolArchive, String> result = parser.parseArchive(nvra);
        Assert.assertEquals(
                new OToolArchive(
                        JDK_8_PACKAGE_NAME,
                        VERSION,
                        CHANGE_SET,
                        "",
                        PROJECT_NAME_U,
                        new ArrayList<Tuple<String, String>>() {{
                            add(new Tuple<>(DEBUG_MODE, RELEASE));
                            add(new Tuple<>(JVM, HOTSPOT));
                        }},
                        F_29_X64,
                        SUFFIX
                ),
                result.getValue()
        );
    }

    @Test
    public void parseValidNVRAWithoutGarbageWithMissingVariant() {
        final String nvra = VALID_NAME_VERSION + "-" + VALID_RELEASE + "." + VALID_ARCHIVE_MISSING;
        final Result<OToolArchive, String> result = parser.parseArchive(nvra);
        Assert.assertEquals(
                new OToolArchive(
                        JDK_8_PACKAGE_NAME,
                        VERSION,
                        CHANGE_SET,
                        "",
                        PROJECT_NAME_U,
                        new ArrayList<Tuple<String, String>>() {{
                            add(new Tuple<>(DEBUG_MODE, FASTDEBUG));
                            add(new Tuple<>(JVM, HOTSPOT));
                        }},
                        F_29_X64,
                        SUFFIX
                ),
                result.getValue()
        );
    }

    @Test
    public void parseValidNVRAWithGarbage() {
        final String nvra = VALID_NAME_VERSION + "-" + VALID_RELEASE_WITH_CHAOS + "." + VALID_ARCHIVE_FULL;
        final Result<OToolArchive, String> result = parser.parseArchive(nvra);
        Assert.assertEquals(
                new OToolArchive(
                        JDK_8_PACKAGE_NAME,
                        VERSION,
                        CHANGE_SET,
                        CHAOS,
                        PROJECT_NAME_U,
                        new ArrayList<Tuple<String, String>>() {{
                            add(new Tuple<>(DEBUG_MODE, RELEASE));
                            add(new Tuple<>(JVM, HOTSPOT));
                        }},
                        F_29_X64,
                        SUFFIX
                ),
                result.getValue()
        );
    }

    @Test
    public void parseValidNVRWithGarbageAndMissingVariant() {
        final String nvra = VALID_NAME_VERSION + "-" + VALID_RELEASE_WITH_CHAOS + "." + VALID_ARCHIVE_MISSING;
        final Result<OToolArchive, String> result = parser.parseArchive(nvra);
        Assert.assertEquals(
                new OToolArchive(
                        JDK_8_PACKAGE_NAME,
                        VERSION,
                        CHANGE_SET,
                        CHAOS,
                        PROJECT_NAME_U,
                        new ArrayList<Tuple<String, String>>() {{
                            add(new Tuple<>(DEBUG_MODE, FASTDEBUG));
                            add(new Tuple<>(JVM, HOTSPOT));
                        }},
                        F_29_X64,
                        SUFFIX
                ),
                result.getValue()
        );
    }
}
