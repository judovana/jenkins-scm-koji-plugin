package org.fakekoji.jobmanager.model;


import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.storage.StorageException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class JDKTestProjectManagerTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File scriptsRoot;

    @Before
    public void setup() throws IOException {
        scriptsRoot = temporaryFolder.newFolder();
    }

    @Test
    public void regenerateAllJDKTestProject() throws IOException, ManagementException, StorageException {
        DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final ConfigManager cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        JDKTestProjectManager tpm = new JDKTestProjectManager(cm.getJdkTestProjectStorage(), new JenkinsJobUpdater(DataGenerator.getSettings(folderHolder)));
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = tpm.regenerateAll();//create all
            JobUpdateResults r2 = tpm.regenerateAll();//re create all
            Assert.assertEquals(0, r1.jobsRewritten.size());
            //Assert.assertNotEquals(0, r1.jobsCreated.size()); todo fix when generator generates test only jobs
            //Assert.assertNotEquals(0, r2.jobsRewritten.size()); todo fix when generator generates test only jobs
            Assert.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }

}
