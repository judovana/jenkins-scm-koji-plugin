package org.fakekoji.core;

import hudson.plugins.scm.koji.model.Build;
import org.fakekoji.DataGenerator;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import static org.fakekoji.DataGenerator.PROJECT_NAME_U;

public class FakeKojiDBTest {

    @TempDir
    static Path temporaryFolder;

    private FakeKojiDB db;

    @BeforeEach
    public void setup() throws IOException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFoldersFromTmpFolder(temporaryFolder.toFile());
        DataGenerator.initBuildsRoot(folderHolder.buildsRoot);
        db = new FakeKojiDB(DataGenerator.getSettings(folderHolder));
    }

    @Test
    public void getBuildsForBuildJobWhenAllBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=release buildPlatform=f29.x86_64 jreSdk=sdk",
                "src",
                false
        ));
        Assertions.assertEquals(
                0,
                builds.size()
        );
        Assertions.assertTrue(builds.stream().allMatch(containsArch("src")));
    }

    @Test
    public void getBuildsForBuildJobWhenSomeBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=fastdebug buildPlatform=f29.x86_64 jreSdk=sdk",
                "src",
                false
        ));
        Assertions.assertEquals(
                2,
                builds.size()
        );
        Assertions.assertTrue(builds.stream().allMatch(containsArch("src")));
    }

    @Test
    public void getBuildsForBuildJobWhenNoneBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug buildPlatform=f29.x86_64 jreSdk=sdk",
                "src",
                false
        ));
        Assertions.assertEquals(
                4,
                builds.size()
        );
        Assertions.assertTrue(builds.stream().allMatch(containsArch("src")));
    }

    @Test
    public void getBuildsForTestJobWhenAllBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=release jreSdk=sdk",
                "f29.x86_64",
                true
        ));
        Assertions.assertEquals(
                4,
                builds.size()
        );
        Assertions.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    @Test
    public void getBuildsForTestJobWhenSomeBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=fastdebug jreSdk=sdk",
                "f29.x86_64",
                true
        ));
        Assertions.assertEquals(
                2,
                builds.size()
        );
        Assertions.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    @Test
    public void getBuildsForTestJobWhenNoneBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug jreSdk=sdk",
                "f29.x86_64",
                true
        ));
        Assertions.assertEquals(
                0,
                builds.size()
        );
        Assertions.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    @Test
    public void getBuildsWithSourcesForTestJobWhenNoneBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug jreSdk=sdk",
                "f29.x86_64 src",
                false
        ));
        Assertions.assertEquals(
                0,
                builds.size()
        );
    }

    @Test
    public void getBuildsWithSourcesForTestJobWhenAllBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=release jreSdk=sdk",
                "f29.x86_64 src",
                false
        ));
        Assertions.assertEquals(
                4,
                builds.size()
        );
        Assertions.assertTrue(builds.stream().allMatch(containsArch("src")));
        Assertions.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    private Predicate<Build> containsArch(final String arch) {
        return build -> build.getRpms().stream().anyMatch(rpm -> rpm.getArch().equals(arch));
    }
}
