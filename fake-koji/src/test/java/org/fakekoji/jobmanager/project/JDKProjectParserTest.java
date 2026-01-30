package org.fakekoji.jobmanager.project;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Job;
import org.fakekoji.storage.StorageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;


public class JDKProjectParserTest {

    @TempDir
    static Path temporaryFolder;

    private AccessibleSettings settings;

    @BeforeEach
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(DataGenerator.initFoldersFromTmpFolder(temporaryFolder.toFile()));
    }

    @Test
    public void parseJDKTestProject() throws StorageException, ManagementException {
        final JDKTestProject jdkTestProject = DataGenerator.getJDKTestProject();
        final JDKProjectParser parser = settings.getJdkProjectParser();
        final Set<Job> actualJobs = parser.parse(jdkTestProject);
        Assertions.assertEquals(
                DataGenerator.getJDKTestProjectJobs(),
                actualJobs
        );
    }

    @Test
    public void parseJDKProject() throws StorageException, ManagementException {
        final JDKProject jdkProject = DataGenerator.getJDKProject();
        final JDKProjectParser parser = settings.getJdkProjectParser();
        final Set<Job> actualJobs = parser.parse(jdkProject);
        Assertions.assertEquals(
                DataGenerator.getJDKProjectJobs(),
                actualJobs
        );
    }
}
