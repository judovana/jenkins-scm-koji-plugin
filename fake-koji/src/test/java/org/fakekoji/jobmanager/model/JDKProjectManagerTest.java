package org.fakekoji.jobmanager.model;


import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.storage.StorageException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class JDKProjectManagerTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File scriptsRoot;

    @Before
    public void setup() throws IOException {
        scriptsRoot = temporaryFolder.newFolder();
    }

    @Test
    public void regenerateAllJDKProject() throws IOException, ManagementException, StorageException {
        DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final ConfigManager cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        JDKProjectManager tpm = new JDKProjectManager(
                cm,
                new JenkinsJobUpdater(DataGenerator.getSettings(folderHolder)),
                folderHolder.reposRoot,
                folderHolder.scriptsRoot
        );
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = tpm.regenerateAll(null);//create all
            JobUpdateResults r2 = tpm.regenerateAll(null);//re create all
            Assert.assertEquals(0, r1.jobsRewritten.size());
            Assert.assertNotEquals(0, r1.jobsCreated.size());
            Assert.assertNotEquals(0, r2.jobsRewritten.size());
            Assert.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }
}
