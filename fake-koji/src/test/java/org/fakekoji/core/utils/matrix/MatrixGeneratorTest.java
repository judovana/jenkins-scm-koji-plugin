package org.fakekoji.core.utils.matrix;


import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.storage.StorageException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MatrixGeneratorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File scriptsRoot;

    @Before
    public void setup() throws IOException {
        scriptsRoot = temporaryFolder.newFolder();
    }

    @Test
    public void showFullMatrix() throws IOException, ManagementException, StorageException {
        DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(temporaryFolder);
        final ConfigManager cm = ConfigManager.create(folderHolder.configsRoot.getAbsolutePath());
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        MatrixGenerator m = new MatrixGenerator(settings, cm);
        List<BuildSpec> bs = m.getBuilds();
        List<TestSpec> ts = m.getTests();
        int t1 = m.printMatrix(System.out, bs, ts, false, false);
        System.out.println(t1 + "/" + (bs.size() * ts.size()));
        int t2 = m.printMatrix(System.out, ts, bs, true, true);
        System.out.println(t2 + "/" + (bs.size() * ts.size()));


    }
}
