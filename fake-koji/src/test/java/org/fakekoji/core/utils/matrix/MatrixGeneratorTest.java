package org.fakekoji.core.utils.matrix;


import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigCache;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.JDKTestProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.storage.StorageException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MatrixGeneratorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AccessibleSettings settings;
    private TableFormatter tf = new TableFormatter.PlainTextTableFormatter();
    @Before
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(DataGenerator.initFolders(temporaryFolder));
    }

    @Test
    public void showFullMatrix() throws ManagementException, StorageException {
        final ConfigManager configManager = settings.getConfigManager();
        MatrixGenerator m = new MatrixGenerator(configManager, new String[0]);
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();
        m.printMatrix(System.out, bs, ts, false, false, tf);
        m.printMatrix(System.out, ts, bs, true, true, tf);
    }

    @Test
    public void showPerProjectMatrix() throws ManagementException, StorageException {
        final ConfigManager configManager = settings.getConfigManager();
        final ConfigCache configCache = new ConfigCache(configManager);
        for (Project project : configCache.getProjects()) {
            MatrixGenerator m = new MatrixGenerator(configManager, new String[]{project.getId()});
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(System.out, ts, bs, true, true, tf);
        }
    }

    @Test
    public void showPerProjectsMatrix() throws ManagementException, StorageException {
        final ConfigManager configManager = settings.getConfigManager();
        final ConfigCache configCache = new ConfigCache(configManager);
        final List<String> jdkProjectNames = configCache.getJdkProjects().stream().map(JDKProject::getId).collect(Collectors.toList());
        final List<String> jdkTestProjectNames = configCache.getJdkTestProjects().stream().map(JDKTestProject::getId).collect(Collectors.toList());
        final List<List<String>> jdkProjectLists = Arrays.asList(jdkProjectNames, jdkTestProjectNames);
        for (List<String> projectNames : jdkProjectLists) {
            MatrixGenerator m = new MatrixGenerator(configManager, projectNames.toArray(new String[0]));
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(System.out, ts, bs, true, true, tf);
        }


    }
}
