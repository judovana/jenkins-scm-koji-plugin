package org.fakekoji.api.xmlrpc;

import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.FakeKojiDB;
import org.fakekoji.jobmanager.JenkinsJobTemplateBuilder;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildList;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fakekoji.DataGenerator.RELEASE_1;
import static org.fakekoji.DataGenerator.RELEASE_2;
import static org.fakekoji.DataGenerator.SUFFIX;

public class NewApiTests {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static FakeKojiDB kojiDB;

    @BeforeClass
    public static void setup() throws IOException {
        final File builds = temporaryFolder.newFolder("builds");
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        DataGenerator.initBuildsRoot(builds);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);

        kojiDB = new FakeKojiDB(settings);
    }

    @Test
    public void getSourcesOfBuilt() {
        final int expectedNumberOfBuilds = 0;
        final GetBuildList params = new GetBuildList(
                DataGenerator.PROJECT_NAME_U,
                "jvm=hotspot debugMode=release buildPlatform=f29.x86_64",
                JenkinsJobTemplateBuilder.SOURCES,
                false
        );
        List<Build> buildList = kojiDB.getBuildList(params);
        Assert.assertEquals(
                getBuildNumberMessage(expectedNumberOfBuilds, buildList.size()),
                expectedNumberOfBuilds,
                buildList.size()
        );
    }

    @Test
    public void getSourcesOfPartiallyNotBuilt() {
        final int expectedNumberOfBuilds = 2;
        final Set<String> expectedArchives = new HashSet<>(Arrays.asList(
                "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.src" + SUFFIX,
                "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.src" + SUFFIX

        ));
        final GetBuildList params = new GetBuildList(
                DataGenerator.PROJECT_NAME_U,
                "jvm=hotspot debugMode=fastdebug buildPlatform=f29.x86_64",
                JenkinsJobTemplateBuilder.SOURCES,
                false
        );
        List<Build> buildList = kojiDB.getBuildList(params);
        Assert.assertEquals(
                getBuildNumberMessage(expectedNumberOfBuilds, buildList.size()),
                expectedNumberOfBuilds,
                buildList.size()
        );
        for (final Build build : buildList) {
            Assert.assertEquals(1, build.getRpms().size());
            final RPM rpm = build.getRpms().get(0);
            Assert.assertTrue(expectedArchives.contains(rpm.getFilename(SUFFIX)));
        }
    }

    @Test
    public void getSourcesOfNotBuiltAtAll() {
        final int expectedNumberOfBuilds = 4;
        final Set<String> expectedArchives = new HashSet<>(Arrays.asList(
                "java-1.8.0-openjdk-version1-" + RELEASE_1 + ".uName.src" + SUFFIX,
                "java-1.8.0-openjdk-version1-" + RELEASE_2 + ".uName.src" + SUFFIX,
                "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.src" + SUFFIX,
                "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.src" + SUFFIX

        ));
        final GetBuildList params = new GetBuildList(
                DataGenerator.PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug buildPlatform=f29.x86_64",
                JenkinsJobTemplateBuilder.SOURCES,
                false
        );
        List<Build> buildList = kojiDB.getBuildList(params);
        Assert.assertEquals(
                getBuildNumberMessage(expectedNumberOfBuilds, buildList.size()),
                expectedNumberOfBuilds,
                buildList.size()
        );
        for (final Build build : buildList) {
            Assert.assertEquals(1, build.getRpms().size());
            final RPM rpm = build.getRpms().get(0);
            Assert.assertTrue(expectedArchives.contains(rpm.getFilename(SUFFIX)));
        }
    }

    @Test
    public void getArchiveOfBuilt() {
        final int expectedNumberOfBuilds = 4;
        final Set<String> expectedArchives = new HashSet<>(Arrays.asList(
                "java-1.8.0-openjdk-version1-" + RELEASE_1 + ".uName.release.hotspot.f29.x86_64" + SUFFIX,
                "java-1.8.0-openjdk-version1-" + RELEASE_2 + ".uName.release.hotspot.f29.x86_64" + SUFFIX,
                "java-1.8.0-openjdk-version2-" + RELEASE_1 + ".uName.release.hotspot.f29.x86_64" + SUFFIX,
                "java-1.8.0-openjdk-version2-" + RELEASE_2 + ".uName.release.hotspot.f29.x86_64" + SUFFIX

        ));
        final GetBuildList params = new GetBuildList(
                DataGenerator.PROJECT_NAME_U,
                "jvm=hotspot debugMode=release",
                "f29.x86_64",
                true
        );
        List<Build> buildList = kojiDB.getBuildList(params);
        Assert.assertEquals(
                getBuildNumberMessage(expectedNumberOfBuilds, buildList.size()),
                expectedNumberOfBuilds,
                buildList.size()
        );
        for (final Build build : buildList) {
            Assert.assertEquals(1, build.getRpms().size());
            final RPM rpm = build.getRpms().get(0);
            Assert.assertTrue(expectedArchives.contains(rpm.getFilename(SUFFIX)));
        }
    }

    @Test
    public void getArchiveOfPartiallyNotBuilt() {
        final int expectedNumberOfBuilds = 2;
        final Set<String> expectedArchives = new HashSet<>(Arrays.asList(
                "java-1.8.0-openjdk-version1-" + RELEASE_1 + ".uName.fastdebug.hotspot.f29.x86_64" + SUFFIX,
                "java-1.8.0-openjdk-version1-" + RELEASE_2 + ".uName.fastdebug.hotspot.f29.x86_64" + SUFFIX

        ));
        final GetBuildList params = new GetBuildList(
                DataGenerator.PROJECT_NAME_U,
                "jvm=hotspot debugMode=fastdebug",
                "f29.x86_64",
                true
        );
        List<Build> buildList = kojiDB.getBuildList(params);
        Assert.assertEquals(
                getBuildNumberMessage(expectedNumberOfBuilds, buildList.size()),
                expectedNumberOfBuilds,
                buildList.size()
        );
        for (final Build build : buildList) {
            Assert.assertEquals(1, build.getRpms().size());
            final RPM rpm = build.getRpms().get(0);
            Assert.assertTrue(expectedArchives.contains(rpm.getFilename(SUFFIX)));
        }
    }

    @Test
    public void getArchiveOfNotBuiltAtAll() {
        final int expectedNumberOfBuilds = 0;
        final GetBuildList params = new GetBuildList(
                DataGenerator.PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug",
                "f29.x86_64",
                false
        );
        List<Build> buildList = kojiDB.getBuildList(params);
        Assert.assertEquals(
                getBuildNumberMessage(expectedNumberOfBuilds, buildList.size()),
                expectedNumberOfBuilds,
                buildList.size()
        );
    }

    private String getBuildNumberMessage(final int expected, final int actual) {
        return "Expected number of builds: " + expected + ", got: " + actual;
    }
}
