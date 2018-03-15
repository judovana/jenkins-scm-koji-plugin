/*
 * The MIT License
 *
 * Copyright 2018 zzambers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fakekoji.xmlrpc.server.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import org.fakekoji.http.AccessibleSettings;
import org.fakekoji.JavaServer;

public class FakeKojiTestUtil {

    private static void createDir(File dir) {
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    private static void createFile(File file, String content) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
            try (PrintStream ps = new PrintStream(new FileOutputStream(file))) {
                ps.println(content);
            }
        }
    }

    /* Because fake-koji tests if file is > 5 bytes */
    private static void createNonEmptyFile(File file) throws IOException {
        createFile(file, "Ententýky dva špalíky, čert vyletěl z elektriky!\n");
    }

    public static void generateBuilds(File root, String n, String v, String r, String a, String... builds) throws Exception {
        createDir(root);
        File nDir = new File(root, n);
        createDir(nDir);
        File vDir = new File(nDir, v);
        createDir(vDir);
        File rDir = new File(vDir, r);
        createDir(rDir);
        File aDir = new File(rDir, a);
        createDir(aDir);
        for (String build : builds) {
            File buildFile = new File(aDir, build);
            createNonEmptyFile(buildFile);
        }
        File dataDir = new File(rDir, "data");
        createDir(dataDir);
        File logsDir = new File(dataDir, "logs");
        createDir(logsDir);
        File logsADir = new File(logsDir, a);
        createDir(logsADir);

        File buildLogFile = new File(logsADir, "build.log");
        if (!buildLogFile.exists()) {
            createNonEmptyFile(buildLogFile);
        }
    }

    public static void generateUpstreamRepo(File root, String repoName) throws Exception {
        if (!root.exists()) {
            root.mkdir();
        }
        File repoDir = new File(root, repoName);
        if (!repoDir.exists()) {
            repoDir.mkdir();
        }
    }

    public static void generateFakeKojiData(File localBuilds, File upstreamRepos) throws Exception {
        String n = "java-1.8.0-openjdk";
        String v = "jdk8u121.b13";
        String rbase = "52.dev";
        String r1 = rbase + ".upstream";
        String r2 = rbase + ".upstream.fastdebug";
        String a1 = "src";
        String a2 = "x86_64";
        String a3 = "win";
        String a4 = "i686";

        String suffix = ".tarxz";

        String buildCommon = n + "-" + v + "-" + rbase;
        String build11 = buildCommon + ".upstream." + a1 + suffix;
        String build12 = buildCommon + ".static." + a2 + suffix;
        String build13 = buildCommon + ".static." + a3 + suffix;
        String build14 = buildCommon + ".static." + a4 + suffix;

        String build21 = buildCommon + ".upstream.fastdebug." + a1 + suffix;
        String build22 = buildCommon + ".static.fastdebug." + a2 + suffix;
        String build23 = buildCommon + ".static.fastdebug." + a3 + suffix;
        String build24 = buildCommon + ".static.fastdebug." + a4 + suffix;

        /* upstream builds */
        generateBuilds(localBuilds, n, v, r1, a1, build11);
        generateBuilds(localBuilds, n, v, r1, a2, build12);
        generateBuilds(localBuilds, n, v, r1, a3, build13);
        generateBuilds(localBuilds, n, v, r1, a4, build14);

        /* fastdebug builds */
        generateBuilds(localBuilds, n, v, r2, a1, build21);
        generateBuilds(localBuilds, n, v, r2, a2, build22);
        generateBuilds(localBuilds, n, v, r2, a3, build23);
        generateBuilds(localBuilds, n, v, r2, a4, build24);

        /* create link for windows builds */
        File nFile = new File(localBuilds, n);
        File winLinkFile = new File(localBuilds, "openjdk8-win");
        Files.createSymbolicLink(winLinkFile.toPath(), localBuilds.toPath().relativize(nFile.toPath()));

        File expectedArches = new File(localBuilds, "java-1.8.0-openjdk-arches-expected");
        createFile(expectedArches, a2 + " " + a3 + " " + a4 + "\n");

        generateUpstreamRepo(upstreamRepos, "java-1.8.0-openjdk-dev");
    }

    public static int getFakeKojiPreviewPort() {
        return 8080;
    }

    public static int getFakeKojiRpcPort() {
        return JavaServer.DFAULT_RP2C_PORT;
    }

    public static int getFakeKojiDownloadPort() {
        return JavaServer.DFAULT_DWNLD_PORT;
    }

    public static JavaServer createDefaultFakeKojiServer(File localBuilds, File upstreamRepos) throws UnknownHostException, MalformedURLException {
        int previewPort = getFakeKojiPreviewPort();
        int rpcPort = getFakeKojiRpcPort();
        int downloadPort = getFakeKojiDownloadPort();
        int sshUploadPort = JavaServer.DFAULT_SSHUPLOAD_PORT;

        AccessibleSettings settings = new AccessibleSettings(localBuilds, upstreamRepos,
                rpcPort, downloadPort, sshUploadPort, previewPort, 8080);
        JavaServer javaServer = new JavaServer(settings);
        return javaServer;
    }

    public static JavaServer createDefaultFakeKojiServerWithData(File tmpDir) throws Exception {
        File localBuilds = new File(tmpDir, "local-builds");
        File upstreamRepos = new File(tmpDir, "upstream-repos");
        generateFakeKojiData(localBuilds, upstreamRepos);
        return createDefaultFakeKojiServer(localBuilds, upstreamRepos);
    }

    public static int doHttpRequest(String urlString, String requestMethod) throws Exception {
        /* see: https://stackoverflow.com/questions/4596447/check-if-file-exists-on-remote-server-using-its-url */
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(requestMethod);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            return connection.getResponseCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
