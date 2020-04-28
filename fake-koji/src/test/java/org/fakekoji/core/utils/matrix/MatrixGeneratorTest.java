package org.fakekoji.core.utils.matrix;


import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.JDKProject;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.jobmanager.project.JDKProjectManager;
import org.fakekoji.jobmanager.project.JDKTestProjectManager;
import org.fakekoji.storage.StorageException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MatrixGeneratorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File scriptsRoot;
    private TableFormatter tf = new TableFormatter.PlainTextTableFormatter();
    @Before
    public void setup() throws IOException {
        scriptsRoot = temporaryFolder.newFolder();
    }

    @Test
    public void showFullMatrix() throws IOException, ManagementException, StorageException {
        DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final ConfigManager cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        MatrixGenerator m = new MatrixGenerator(settings, cm, new String[0]);
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();
        m.printMatrix(System.out, bs, ts, false, false, tf);
        m.printMatrix(System.out, ts, bs, true, true, tf);
    }

    @Test
    public void showPerProjectMatrix() throws IOException, ManagementException, StorageException {
        DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final ConfigManager cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);

        final JenkinsJobUpdater jenkinsJobUpdater = new JenkinsJobUpdater(settings);
        JDKTestProjectManager jdkTestProjectManager = new JDKTestProjectManager(
                cm.getJdkTestProjectStorage()
        );
        JDKProjectManager jdkProjectManager = new JDKProjectManager(
                cm,
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
        for (Project project : MatrixGenerator.concateProjects(jdkProjectManager.readAll(), jdkTestProjectManager.readAll())) {
            MatrixGenerator m = new MatrixGenerator(settings, cm, new String[]{project.getId()});
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(System.out, ts, bs, true, true, tf);
        }
    }

    @Test
    public void showPerProjectsMatrix() throws IOException, ManagementException, StorageException {
        DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final ConfigManager cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);

        final JenkinsJobUpdater jenkinsJobUpdater = new JenkinsJobUpdater(settings);
        JDKTestProjectManager jdkTestProjectManager = new JDKTestProjectManager(
                cm.getJdkTestProjectStorage()
        );
        JDKProjectManager jdkProjectManager = new JDKProjectManager(
                cm,
                settings.getLocalReposRoot(),
                settings.getScriptsRoot()
        );
        String[] all1 = toStringList(jdkProjectManager.readAll()).toArray(new String[0]);
        String[] all2 = toStringList(jdkTestProjectManager.readAll()).toArray(new String[0]);
        String[][] all = new String[][]{all1, all2};
        for (String[] a : all) {
            MatrixGenerator m = new MatrixGenerator(settings, cm, a);
            List<BuildSpec> bs = m.getBuilds();
            List<TestSpec> ts = m.getTests();
            m.printMatrix(System.out, ts, bs, true, true, tf);
        }


    }

    private Collection<String> toStringList(List<? extends Project> projects) {
        Collection<String> r = new ArrayList<>(projects.size());
        for (Project project : projects) {
            r.add(project.getId());
        }
        return r;
    }
}
