/*
 * The MIT License
 *
 * Copyright 2018 .
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
package org.fakekoji.core;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.fakekoji.jobmanager.JenkinsJobUpdater;
import org.fakekoji.jobmanager.JobUpdater;
import org.fakekoji.jobmanager.ConfigManager;
import org.fakekoji.jobmanager.project.JDKProjectParser;
import org.fakekoji.jobmanager.project.ReverseJDKProjectParser;
import org.fakekoji.xmlrpc.server.JavaServerConstants;

public class AccessibleSettings {

    public static class Master {
        public final String label = "Hydra";
        public final String machine = "hydra";
        public final String domain = "brq.redhat.com";
        public final String fullName = machine + "." + domain;
        public final String protocol = "http";
        public final String baseUrl = protocol + "://" + fullName;
    }

    public static final Master master = new Master();

    public static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private final File dbFileRoot;
    private final File localReposRoot;
    private final File configRoot;
    private final File resultsFile;
    private final File jenkinsJobsRoot;
    private final File jenkinsJobArchiveRoot;
    private final File scriptsRoot;

    private final URL jenkins;

    private final int xmlRpcPort;
    private final int fileDownloadPort;
    private final int sshPort;
    private final int webappPort;
    private final ConfigManager configManager;
    private final JDKProjectParser jdkProjectParser;
    private final ReverseJDKProjectParser reverseJDKProjectParser;
    private final JobUpdater jenkinsJobUpdater;
    private ProjectMapping projectMapping;

    private final List<String> reportParams;
    private final List<String> reportChartParams;

    public AccessibleSettings(
            File dbFileRoot,
            File localReposRoot,
            File configRoot,
            File jenkinsJobsRoot,
            File jenkinsJobArchiveRoot,
            File scriptsRoot,
            final URL jenkins,
            int xmlRpcPort,
            int fileDownloadPort,
            int sshPort,
            int webappPort,
            List<String> reportParams,
            List<String> reportChartParams
    ) {
        this.dbFileRoot = dbFileRoot;
        this.localReposRoot = localReposRoot;
        this.configRoot = configRoot;
        this.resultsFile = new File(configRoot, "results.db");
        this.jenkinsJobsRoot = jenkinsJobsRoot;
        this.jenkinsJobArchiveRoot = jenkinsJobArchiveRoot;
        this.scriptsRoot = scriptsRoot;
        this.jenkins = jenkins;
        this.xmlRpcPort = xmlRpcPort;
        this.fileDownloadPort = fileDownloadPort;
        this.sshPort = sshPort;
        this.webappPort = webappPort;
        configManager = new ConfigManager(this);
        jdkProjectParser = new JDKProjectParser(
                configManager,
                localReposRoot,
                scriptsRoot
        );
        reverseJDKProjectParser = new ReverseJDKProjectParser();
        jenkinsJobUpdater = new JenkinsJobUpdater(
                configManager,
                jdkProjectParser,
                jenkinsJobsRoot,
                jenkinsJobArchiveRoot
        );
        this.projectMapping = new ProjectMapping(this);
        this.reportParams = reportParams;
        this.reportChartParams = reportChartParams;
    }

    public File getDbFileRoot() {
        warn(dbFileRoot, "dbFileRoot");
        return dbFileRoot;
    }

    public File getLocalReposRoot() {
        return localReposRoot;
    }

    public File getConfigRoot() {
        warn(configRoot, "configRoot");
        return configRoot;
    }

    public File getResultsFile() {
        warn(resultsFile, "resultsFile");
        return resultsFile;
    }

    public File getJenkinsJobsRoot() {
        warn(jenkinsJobsRoot, "jenkinsJobsRoot");
        return jenkinsJobsRoot;
    }

    public File getJenkinsJobArchiveRoot() {
        warn(jenkinsJobArchiveRoot, "jenkinsJobArchiveRoot");
        return jenkinsJobArchiveRoot;
    }

    public File getScriptsRoot() {
        warn(scriptsRoot, "scriptsRoot");
        return scriptsRoot;
    }

    public URL getJenkins() {
        return jenkins;
    }

    public String getJenkinsUrl() {
        return jenkins.toString();
    }
    
    public int getJenkinsPort() {
        return jenkins.getPort();
    }

    public int getXmlRpcPort() {
        return xmlRpcPort;
    }

    public int getFileDownloadPort() {
        return fileDownloadPort;
    }

    public int getSshPort() {
        return sshPort;
    }

    public int getWebappPort() {
        return webappPort;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public JDKProjectParser getJdkProjectParser() {
        return jdkProjectParser;
    }

    public ReverseJDKProjectParser getReverseJDKProjectParser() {
        return reverseJDKProjectParser;
    }

    public JobUpdater getJobUpdater() {
        return jenkinsJobUpdater;
    }

    public ProjectMapping getProjectMapping() {
        return projectMapping;
    }

    private void warn(File f, String src) {
        if (f == null) {
            LOGGER.log(Level.SEVERE, "Reqested file for `{0}` is NULL", src);
        } else if (!f.exists()) {
            LOGGER.log(Level.SEVERE, "Reqested file `{0}` for `{1}` do not exists. You can expect failure", new Object[]{f.getAbsolutePath(), src});
        }
    }

    public List<String> getReportChartParams() {
        return reportChartParams;
    }

    public List<String> getReportParams() {
        return reportParams;
    }
}
