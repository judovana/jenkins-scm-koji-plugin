package org.fakekoji.jobmanager;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.JobUpdateResults;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.storage.StorageException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class OToolTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AccessibleSettings settings;
    private JobUpdater jobUpdater;
    private ConfigManager cm;

    @Before
    public void setup() throws IOException, StorageException {
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        settings = DataGenerator.getSettings(folderHolder);
        jobUpdater = new JenkinsJobUpdater(settings);
    }

    @Test
    public void regenerateAllJDKTestProjectJobs() throws ManagementException, StorageException {
        final JobUpdater jobUpdater = new JenkinsJobUpdater(settings);
        final JDKTestProjectManager manager = new JDKTestProjectManager(cm.getJdkTestProjectStorage());
        final OTool tpm = new OTool(jobUpdater);
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = tpm.regenerateAll(null, manager); // create all
            JobUpdateResults r2 = tpm.regenerateAll(null, manager); // recreate all
            Assert.assertEquals(0, r1.jobsRewritten.size());
            //Assert.assertNotEquals(0, r1.jobsCreated.size()); todo fix when generator generates test only jobs
            //Assert.assertNotEquals(0, r2.jobsRewritten.size()); todo fix when generator generates test only jobs
            Assert.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }

    @Test
    public void regenerateAllJDKProjectJobs() throws ManagementException, StorageException {
        final JDKProjectManager manager = new JDKProjectManager(
                cm,
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
        final OTool tpm = new OTool(jobUpdater);
        JenkinsCliWrapper.killCli();
        try {
            JobUpdateResults r1 = tpm.regenerateAll(null, manager); // create all
            JobUpdateResults r2 = tpm.regenerateAll(null, manager); // recreate all
            Assert.assertEquals(0, r1.jobsRewritten.size());
            Assert.assertNotEquals(0, r1.jobsCreated.size());
            Assert.assertNotEquals(0, r2.jobsRewritten.size());
            Assert.assertEquals(0, r2.jobsCreated.size());
        } finally {
            JenkinsCliWrapper.reinitCli();
        }
    }
}
