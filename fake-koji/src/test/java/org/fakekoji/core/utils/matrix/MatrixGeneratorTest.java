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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MatrixGeneratorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AccessibleSettings settings;
    private TableFormatter tf = new TableFormatter.PlainTextTableFormatter();//new TableFormatter.HtmlTableFormatter(true);

    @Before
    public void setup() throws IOException {
        settings = DataGenerator.getSettings(DataGenerator.initFolders(temporaryFolder));
    }

    @Test
    public void showFullMatrix() throws ManagementException, StorageException, IOException {
        final ConfigManager configManager = settings.getConfigManager();
        MatrixGenerator m = new MatrixGenerator(configManager, new String[0]);
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();

        final File matrixFile = temporaryFolder.newFile();
        final String expectedContent = readResource("full_dropped_matrix");
        m.printMatrix(new PrintStream(matrixFile), ts, bs, true, true, tf);
        assertContents(expectedContent, matrixFile);

    }

    @Test
    public void showFullSqueezedMatrix() throws ManagementException, StorageException, IOException {
        final ConfigManager configManager = settings.getConfigManager();
        final File matrixFile = temporaryFolder.newFile();
        final String expectedContent = readResource("squeezed_matrix");
        final PrintStream printStream = new PrintStream(matrixFile);
        MatrixGenerator m = new MatrixGenerator(
                configManager,
                ".*",
                ".*",
                new TestEqualityFilter(true, true, false, true, false),
                new BuildEqualityFilter(true, true, false, true, true, false),
                new String[0]
        );
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();
        m.printMatrix(printStream, bs, ts, true, true, tf);
        m.printMatrix(printStream, ts, bs, false, false, tf);
        assertContents(expectedContent, matrixFile);
    }

    @Test
    public void showPerProjectMatrix() throws ManagementException, StorageException, IOException {
        final ConfigManager configManager = settings.getConfigManager();
        final ConfigCache configCache = new ConfigCache(configManager);
        final File matrixFile = temporaryFolder.newFile();
        final String expectedContent = readResource("per_project_matrices");
        final PrintStream printStream = new PrintStream(matrixFile);
        for (Project project : configCache.getProjects()) {
            MatrixGenerator m = new MatrixGenerator(configManager, new String[]{project.getId()});
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(printStream, ts, bs, true, true, tf);
        }
        assertContents(expectedContent, matrixFile);
    }

    @Test
    public void showPerProjectsMatrix() throws ManagementException, StorageException, IOException {
        final ConfigManager configManager = settings.getConfigManager();
        final ConfigCache configCache = new ConfigCache(configManager);
        final List<String> jdkProjectNames = configCache.getJdkProjects().stream().map(JDKProject::getId).collect(Collectors.toList());
        final List<String> jdkTestProjectNames = configCache.getJdkTestProjects().stream().map(JDKTestProject::getId).collect(Collectors.toList());
        final List<List<String>> jdkProjectLists = Arrays.asList(jdkProjectNames, jdkTestProjectNames);
        final File matrixFile = temporaryFolder.newFile();
        final String expectedContent = readResource("per_projects_matrices");
        final PrintStream printStream = new PrintStream(matrixFile);
        for (List<String> projectNames : jdkProjectLists) {
            MatrixGenerator m = new MatrixGenerator(configManager, projectNames.toArray(new String[0]));
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(printStream, ts, bs, true, true, tf);
        }
        assertContents(expectedContent, matrixFile);
    }

    private void assertContents(final String content1, final File matrixFile) throws IOException {
        Assert.assertEquals(content1, readFileTrimLines(matrixFile));
    }

    private String readFileTrimLines(final InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line.trim()).append('\n');
        }
        return content.toString();
    }

    private String readFileTrimLines(final File file) throws IOException {
        return readFileTrimLines(new FileInputStream(file));
    }

    private String readResource(final String name) throws IOException {
        return readFileTrimLines(this.getClass().getResourceAsStream("/org/fakekoji/core/utils/matrix/" + name));
    }
}
