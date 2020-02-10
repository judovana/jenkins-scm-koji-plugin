package org.fakekoji.api.http.rest;

import org.fakekoji.DataGenerator;
import org.fakekoji.jobmanager.JenkinsCliWrapper;
import org.fakekoji.storage.StorageException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class OToolServiceRun {

    public static void main(String[] args) throws IOException, StorageException {
        JenkinsCliWrapper.killCli();
        final File oTool = Files.createTempDirectory("oTool").toFile();
        final DataGenerator.FolderHolder folderHolder = DataGenerator.initFolders(oTool);

        new OToolService(DataGenerator.getSettings(folderHolder)).start();
    }
}
