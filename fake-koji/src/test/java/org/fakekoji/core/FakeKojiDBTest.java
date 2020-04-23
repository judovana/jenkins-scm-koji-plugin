package org.fakekoji.core;

import hudson.plugins.scm.koji.model.Build;
import org.fakekoji.DataGenerator;
import org.fakekoji.storage.StorageException;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetBuildList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import static org.fakekoji.DataGenerator.PROJECT_NAME_U;

public class FakeKojiDBTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private FakeKojiDB db;

    @Before
    public void setup() throws IOException, StorageException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        DataGenerator.initBuildsRoot(folderHolder.buildsRoot);
        db = new FakeKojiDB(DataGenerator.getSettings(folderHolder));
    }

    @Test
    public void getBuildsForBuildJobWhenAllBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=release buildPlatform=f29.x86_64",
                "src",
                false
        ));
        Assert.assertEquals(
                0,
                builds.size()
        );
        Assert.assertTrue(builds.stream().allMatch(containsArch("src")));
    }

    @Test
    public void getBuildsForBuildJobWhenSomeBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=fastdebug buildPlatform=f29.x86_64",
                "src",
                false
        ));
        Assert.assertEquals(
                2,
                builds.size()
        );
        Assert.assertTrue(builds.stream().allMatch(containsArch("src")));
    }

    @Test
    public void getBuildsForBuildJobWhenNoneBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug buildPlatform=f29.x86_64",
                "src",
                false
        ));
        Assert.assertEquals(
                4,
                builds.size()
        );
        Assert.assertTrue(builds.stream().allMatch(containsArch("src")));
    }

    @Test
    public void getBuildsForTestJobWhenAllBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=release",
                "f29.x86_64",
                true
        ));
        Assert.assertEquals(
                4,
                builds.size()
        );
        Assert.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    @Test
    public void getBuildsForTestJobWhenSomeBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=fastdebug",
                "f29.x86_64",
                true
        ));
        Assert.assertEquals(
                2,
                builds.size()
        );
        Assert.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    @Test
    public void getBuildsForTestJobWhenNoneBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug",
                "f29.x86_64",
                true
        ));
        Assert.assertEquals(
                0,
                builds.size()
        );
        Assert.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    @Test
    public void getBuildsWithSourcesForTestJobWhenNoneBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=slowdebug",
                "f29.x86_64 src",
                false
        ));
        Assert.assertEquals(
                0,
                builds.size()
        );
    }

    @Test
    public void getBuildsWithSourcesForTestJobWhenAllBuilt() {
        final List<Build> builds = db.getBuildList(new GetBuildList(
                PROJECT_NAME_U,
                "jvm=hotspot debugMode=release",
                "f29.x86_64 src",
                false
        ));
        Assert.assertEquals(
                4,
                builds.size()
        );
        Assert.assertTrue(builds.stream().allMatch(containsArch("src")));
        Assert.assertTrue(builds.stream().allMatch(containsArch("f29.x86_64")));
    }

    private Predicate<Build> containsArch(final String arch) {
        return build -> build.getRpms().stream().anyMatch(rpm -> rpm.getArch().equals(arch));
    }
}
