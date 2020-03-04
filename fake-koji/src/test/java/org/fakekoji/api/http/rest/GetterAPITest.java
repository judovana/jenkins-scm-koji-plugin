package org.fakekoji.api.http.rest;

import org.fakekoji.DataGenerator;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.jobmanager.model.Project;
import org.fakekoji.model.JDKVersion;
import org.fakekoji.storage.StorageException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetterAPITest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private static DataGenerator.FolderHolder folderHolder;
    private static AccessibleSettings setting;

    @BeforeClass
    public static void setup() throws IOException, StorageException {
        folderHolder = DataGenerator.initFolders(temporaryFolder);
        setting = DataGenerator.getSettings(folderHolder);
        new OToolService(DataGenerator.getSettings(folderHolder)).start();
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

    private HTTPResponse makeHTTPConnection(final String path) throws IOException {
        final URL url = new URL("http://localhost:8888/get/" + path);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
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
            Assert.assertEquals(status, this.status);
            Assert.assertEquals(body + '\n', this.body);
        }
    }
}
