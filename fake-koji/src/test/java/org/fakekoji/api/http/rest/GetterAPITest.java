package org.fakekoji.api.http.rest;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.storage.StorageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetterAPITest {

    @TempDir
    static Path temporaryFolder;

    private static DataGenerator.FolderHolder folderHolder;
    private static AccessibleSettings setting;

    @BeforeAll
    public static void setup() throws IOException, StorageException, ManagementException {
        folderHolder = DataGenerator.initFoldersFromTmpFolder(temporaryFolder.toFile());
        setting = DataGenerator.getSettings(folderHolder);
        final AccessibleSettings settings = DataGenerator.getSettings(folderHolder);
        new OToolService(settings).start();
    }

    @Test
    public void getXMLRPCPort() throws IOException {
        final String path = "port?service=xmlRpc";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, setting.getXmlRpcPort());
    }

    @Test
    public void getFileDownloadPort() throws IOException {
        final String path = "port?service=fileDownload";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, setting.getFileDownloadPort());
    }

    @Test
    public void getSSHPort() throws IOException {
        final String path = "port?service=ssh";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, setting.getSshPort());
    }

    @Test
    public void getWebappPort() throws IOException {
        final String path = "port?service=webapp";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, setting.getWebappPort());
    }

    @Test
    public void getJDKVersionOfProject() throws IOException {
        final String path = "jdkVersion?project=" + DataGenerator.PROJECT_NAME_U;
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, DataGenerator.getJDKVersion8().getId());
    }

    @Test
    public void getJDKVersionOfProduct() throws IOException {
        final String path = "jdkVersion?product=" + DataGenerator.JDK_8_PACKAGE_NAME;
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, DataGenerator.getJDKVersion8().getId());
    }

    @Test
    public void getJDKVersions() throws IOException {
        final String path = "jdkVersions";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(
                200,
                DataGenerator.getJDKVersions()
                        .stream()
                        .map(JDKVersion::getId)
                        .sorted(String::compareTo)
                        .collect(Collectors.joining("\n"))
        );
    }

    @Test
    public void getProducts() throws IOException {

        final String products = DataGenerator.getJDKVersions()
                .stream()
                .map(JDKVersion::getPackageNames)
                .flatMap(List::stream)
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n"));

        final String path = "products";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, products);
    }

    @Test
    public void getAllProjects() throws IOException {

        final String projects = Stream.of(DataGenerator.getProjects())
                .flatMap(Set::stream)
                .map(Project::getId)
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n"));

        final String path = "projects";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, projects);
    }

    @Test
    public void getJDKProjects() throws IOException {

        final String projects = Stream.of(DataGenerator.getJDKProjects())
                .flatMap(Set::stream)
                .map(project -> (Project) project)
                .map(Project::getId)
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n"));

        final String path = "projects?type=JDK_PROJECT";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, projects);
    }

    @Test
    public void getJDKTestProjects() throws IOException {

        final String projects = Stream.of(DataGenerator.getJDKTestProjects())
                .flatMap(Set::stream)
                .map(project -> (Project) project)
                .map(Project::getId)
                .sorted(String::compareTo)
                .collect(Collectors.joining("\n"));

        final String path = "projects?type=JDK_TEST_PROJECT";
        final HTTPResponse response = makeHTTPConnection(path);
        response.check(200, projects);
    }

    @Test
    public void getBuilds() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("listing"), "utf-8"))) {
            while (true) {
                String s = br.readLine();
                if (s == null) {
                    break;
                }
                File f = new File(folderHolder.buildsRoot.getAbsoluteFile(), s);
                f.getParentFile().mkdirs();
                Files.createFile(f.toPath());
            }
        }
        checkBuildsListing("builds", 100043);
        checkBuildsListing("builds?type=filenames", 100043);
        checkBuildsListing("builds?includeData=true", 101408);
        checkBuildsListing("builds?type=files", 213301);
        checkBuildsListing("builds?type=dirs", 113124);
        checkBuildsListing("builds?type=nvras", 93918);
        checkBuildsListing("builds?type=nvrs", 23026);
    }

    private void checkBuildsListing(String cmd, int check) throws IOException {
        final HTTPResponse response = makeHTTPConnection(cmd);
        Assertions.assertEquals(200, response.status);
        Assertions.assertEquals(check, response.body.length());
    }

    private HTTPResponse makeHTTPConnection(final String path) throws IOException {
        final URL url = new URL("http://localhost:8888/get/" + path);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int timeout = 5000 * 10000;
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestMethod("GET");
        connection.connect();
        final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        final StringBuilder content = new StringBuilder();
        while ((line = in.readLine()) != null) {
            content.append(line).append('\n');
        }
        in.close();
        connection.disconnect();
        return new HTTPResponse(connection.getResponseCode(), content.toString());
    }

    private class HTTPResponse {
        private final int status;
        private final String body;

        private HTTPResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        private void check(final int status, final int body) {
            check(status, String.valueOf(body));
        }

        private void check(final int status, final String body) {
            Assertions.assertEquals(status, this.status);
            Assertions.assertEquals(body + '\n', this.body);
        }
    }
}
