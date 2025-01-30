/*
 * The MIT License
 *
 * Copyright 2015 user.
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
package org.fakekoji.server;

/*

  To deploy, run:
  mvn clean install
  cd target

  java -cp ./fake-koji-...-jar-with-dependencies.jar:logging.jar  org.fakekoji.JavaServer /path/.properties

  where logging.jar is any logging implementation. Apache xmlrpc are happy with apache logging.

 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fakekoji.api.http.rest.OToolService;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.fakekoji.api.http.filehandling.FileDownloadService;
import org.fakekoji.api.xmlrpc.XmlRpcKojiService;
import org.fakekoji.api.ssh.ScpService;

public class JavaServer {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    public static final int DEFAULT_WEBAPP_PORT = 80;

    private final XmlRpcKojiService xmlRpcKojiService;
    private final FileDownloadService fileDownloadService;
    private final ScpService scpService;
    private final OToolService oToolService;

    public JavaServer(AccessibleSettings settings) {
        this(settings, null);
    }

    public JavaServer(AccessibleSettings settings, OToolService oToolService) {
        xmlRpcKojiService = new XmlRpcKojiService(settings);
        fileDownloadService = new FileDownloadService(settings.getDbFileRoot(), settings.getFileDownloadPort());
        scpService = new ScpService(settings);
        this.oToolService = oToolService;
    }

    public void start() throws Exception {
        /* koji xmlRpc server*/
        LOGGER.info("Attempting to start XML-RPC Server...");
        xmlRpcKojiService.start();
        LOGGER.info("Started successfully on " + xmlRpcKojiService.getPort());
        /* koji download server*/
        LOGGER.info("Starting http server to return files.");
        fileDownloadService.start();
        LOGGER.info("Started successfully on " + fileDownloadService.getPort());
        /* ssh server to upload files to fakekoji */
        LOGGER.info("Starting sshd server to accept files.");
        scpService.start();
        LOGGER.info("Sshd server started successfully on " + scpService.getPort());
        if (oToolService != null) {
            LOGGER.info("Starting Rest API service");
            oToolService.start();
            LOGGER.info("Rest API service started successfully on " + oToolService.getPort());
        }
    }

    public void stop() {
        try {
            scpService.stop();
        } catch (IOException ex) {
            Logger.getLogger(JavaServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        fileDownloadService.stop();
        xmlRpcKojiService.stop();
        if (oToolService != null) {
            oToolService.stop();
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            throw new RuntimeException("Expected path to properties file"); // TODO: add description of each property
        }

        final String propertyFilePath = args[0];
        final Properties props = new Properties();
        props.load(new FileInputStream(new File(propertyFilePath)));

        final int xmlRpcPort;
        final int sshPort;
        final int fileDownloadPort;
        final int webappPort;

        xmlRpcPort = getPort(props, Property.XML_RPC_PORT, JavaServerConstants.DFAULT_RP2C_PORT);
        fileDownloadPort = getPort(props, Property.FILE_DOWNLOAD_PORT, xmlRpcPort + 1);
        sshPort = getPort(props, Property.SSH_PORT, xmlRpcPort - 26);
        webappPort = getPort(props, Property.WEBAPP_PORT, DEFAULT_WEBAPP_PORT);

        final AccessibleSettings settings = new AccessibleSettings(
                getRoot(props, Property.BUILD_DB_ROOT),
                getRoot(props, Property.REPOS_ROOT),
                getRoot(props, Property.CONFIGS_ROOT),
                getRoot(props, Property.JENKINS_JOBS_ROOT),
                getRoot(props, Property.JENKINS_JOB_ARCHIVE_ROOT),
                getRoot(props, Property.SCRIPTS_ROOT),
                parseUrl(props, Property.JENKINS_URL),
                getNullableString(props, Property.JENKINS_SSH_HOST),
                getNullableInt(props, Property.JENKINS_SSH_PORT),
                getNullableString(props, Property.JENKINS_SSH_USER),
                getNullableString(props, Property.JENKINS_SSH_KEYPATH),
                parseUrl(props, Property.COMPARE_URL),
                xmlRpcPort,
                fileDownloadPort,
                sshPort,
                webappPort,
                asList(props, Property.REPORT_EXEC_DEFAULTPARAMS.value),
                asList(props, Property.REPORT_EXEC_DEFAULTCHARTPARAM.value)
        );

        final OToolService oToolService = new OToolService(settings);

        new JavaServer(settings, oToolService).start();
    }

    private static List<String> asList(Properties props, String key) {
        String args = props.getProperty(key);
        if (args == null || args.trim().isEmpty()){
            return new ArrayList<>();
        }
        String[] split = args.trim().split("\\s+");
        return Collections.unmodifiableList(Arrays.asList(split));
    }

    private enum Property {
        XML_RPC_PORT("port.xml.rpc"),
        FILE_DOWNLOAD_PORT("port.file.download"),
        SSH_PORT("port.ssh"),
        JENKINS_URL("url.jenkins"),
        JENKINS_SSH_HOST("jenkins.ssh.host"),
        JENKINS_SSH_PORT("jenkins.ssh.port"),
        JENKINS_SSH_USER("jenkins.ssh.user"),
        JENKINS_SSH_KEYPATH("jenkins.ssh.keypath"),
        COMPARE_URL("url.comparator"),
        WEBAPP_PORT("port.webapp"),
        REPOS_ROOT("root.repos"),
        BUILD_DB_ROOT("root.build.db"),
        JENKINS_JOBS_ROOT("root.jenkins.jobs"),
        JENKINS_JOB_ARCHIVE_ROOT("root.jenkins.job.archive"),
        CONFIGS_ROOT("root.configs"),
        SCRIPTS_ROOT("root.scripts"),
        REPORT_EXEC_DEFAULTPARAMS("report.exec.defaultparams"),
        REPORT_EXEC_DEFAULTCHARTPARAM("Sreport.exec.defaultchartparams");

        private final String value;

        Property(final String value) {
            this.value = value;
        }
    }

    private static URL parseUrl(final Properties props, final Property prop) throws MalformedURLException {
        return new URL(props.getProperty(prop.value));
    }

    private static int getPort(Properties props, Property prop, int defaultProp) {
        return Integer.valueOf(props.getProperty(prop.value, String.valueOf(defaultProp)));
    }

    private static Integer getNullableInt(Properties props, Property prop) {
        String s = props.getProperty(prop.value);
        if (s == null || s.equals("null")) {
            return null;
        }
        return Integer.valueOf(s);
    }

    private static String getNullableString(Properties props, Property prop) {
        String s = props.getProperty(prop.value);
        if (s == null || s.equals("null")) {
            return null;
        }
        return s;
    }

    private static File getRoot(Properties props, Property prop) {
        final String rootPath = props.getProperty(prop.value);
        if (rootPath == null) {
            throw new RuntimeException("Property " + prop.value + " not set!\n");
        }
        final File root = new File(rootPath);
        if (!root.exists()) {
            throw new RuntimeException("File " + root.getAbsolutePath() + " does not exist!\n");
        }
        if (!root.isDirectory()) {
            throw new RuntimeException(prop.value + " must be a directory!\n");
        }
        return root;
    }
}
