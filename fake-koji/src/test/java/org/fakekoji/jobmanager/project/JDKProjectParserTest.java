package org.fakekoji.jobmanager.project;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.storage.StorageException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Set;


public class JDKProjectParserTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AccessibleSettings settings;

    @Before
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(DataGenerator.initFolders(temporaryFolder));
    }

    @Test
    public void parseJDKTestProject() throws StorageException, ManagementException {
        final JDKTestProject jdkTestProject = DataGenerator.getJDKTestProject();
        final JDKProjectParser parser = settings.getJdkProjectParser();
        final Set<Job> actualJobs = parser.parse(jdkTestProject);
        Assert.assertEquals(
                "ParsedProject should be equal",
                DataGenerator.getJDKTestProjectJobs(),
                actualJobs
        );
    }

    @Test
    public void parseJDKProject() throws StorageException, ManagementException {
        final JDKProject jdkProject = DataGenerator.getJDKProject();
        final JDKProjectParser parser = settings.getJdkProjectParser();
        final Set<Job> actualJobs = parser.parse(jdkProject);
        Assert.assertEquals(
                "ParsedProject should be equal",
                DataGenerator.getJDKProjectJobs(),
                actualJobs
        );
    }
}
