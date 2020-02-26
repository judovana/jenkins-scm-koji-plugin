package org.fakekoji.core.utils;

import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.model.OToolBuild;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OToolBuildParserTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private static final List<JDKVersion> products = new ArrayList<>(DataGenerator.getJDKVersions());

    private static final List<JDKProject> jdkProjects = Arrays.asList(
            new JDKProject("jdkProject1", null, null, null, null, null, null),
            new JDKProject("jdkProject2", null, null, null, null, null, null),
            new JDKProject("jdkProject3", null, null, null, null, null, null)
    );

    private static OToolBuildParser parser = new OToolBuildParser(
            products,
            jdkProjects
    );

    @Test
    public void parseValidNVRWithNoGarbage() throws ParserException {
        final String nvr = "java-1.8.0-openjdk-version-changeSet.jdkProject1";
        final OToolBuild build = parser.parse(nvr);
        Assert.assertEquals(
                new OToolBuild(
                        "java-1.8.0-openjdk",
                        "version",
                        "changeSet",
                        "",
                        "jdkProject1"
                ),
                build
        );
    }

    @Test
    public void parseValidNVRWithGarbage() throws ParserException {
        final String nvr = "java-1.8.0-openjdk-version-changeSet.G.A.R.B.A.G.E.jdkProject1";
        final OToolBuild build = parser.parse(nvr);
        Assert.assertEquals(
                new OToolBuild(
                        "java-1.8.0-openjdk",
                        "version",
                        "changeSet",
                        "G.A.R.B.A.G.E",
                        "jdkProject1"
                ),
                build
        );
    }

    @Test
    public void parseNVRWithInvalidPackageName() throws ParserException {
        expectedException.expect(ParserException.class);
        expectedException.expectMessage("Unknown package name: C#-1.8.0-C#");
        final String nvr = "C#-1.8.0-C#-version-changeSet.jdkProject1";
        parser.parse(nvr);
    }

    @Test
    public void parseNVRWithMissingPackageName() throws ParserException {
        expectedException.expect(ParserException.class);
        expectedException.expectMessage(OToolBuildParser.ERROR_NVR_SPLIT_LENGTH);
        final String nvr = "version-changeSet.jdkProject1";
        parser.parse(nvr);
    }

    @Test
    public void parseNVRWithMissingVersion() throws ParserException {
        expectedException.expect(ParserException.class);
        expectedException.expectMessage("Unknown package name: java-1.8.0");
        final String nvr = "java-1.8.0-openjdk-changeSet.G.A.R.B.A.G.E.jdkProject1";
        parser.parse(nvr);
    }

    @Test
    public void parseNVRWithInvalidRelease() throws ParserException {
        expectedException.expect(ParserException.class);
        expectedException.expectMessage(OToolBuildParser.ERROR_RELEASE_SPLIT_LENGTH);
        final String nvr = "java-1.8.0-openjdk-version-jdkProject1";
        parser.parse(nvr);
    }
}
