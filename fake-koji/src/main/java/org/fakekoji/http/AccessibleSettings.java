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
package org.fakekoji.http;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 *
 */
public class AccessibleSettings {

    private final File dbFileRoot;
    private final File localReposRoot;

    private final int realXPort;
    private final int realDPort;
    private final int realUPort;
    private final int realJPort;
    private final int previewPort;
    private URL xmlRpcUrl;
    private URL downloadUrl;
    private URL jenkinsUrlString;

    public AccessibleSettings(File dbFileRoot, File localReposRoot, int realXPort, int realDPort, int realUPort, int previewPort, int jenkinsPort) throws UnknownHostException, MalformedURLException {
        this.dbFileRoot = dbFileRoot;
        this.localReposRoot = localReposRoot;
        this.realXPort = realXPort;
        this.realDPort = realDPort;
        this.realUPort = realUPort;
        this.realJPort = jenkinsPort;
        this.previewPort = previewPort;

        String thisMachine = InetAddress.getLocalHost().getHostName();
        this.xmlRpcUrl = new URL("http://" + thisMachine + ":" + realXPort + "/RPC2/");
        this.downloadUrl = new URL("http://" + thisMachine + ":" + realDPort + "/");
        this.jenkinsUrlString = new URL("http://" + thisMachine + ":" + jenkinsPort + "/");
    }

    /**
     * Default may lead to localhost, so itcna be desirable to set proper
     * hostname via this method
     *
     * @param specialHost
     * @throws MalformedURLException
     */
    public void setDownloadUrl(String specialHost) throws MalformedURLException {
        this.downloadUrl = new URL("http://" + specialHost + ":" + realDPort + "/");

    }

    /**
     * Default may lead to localhost, so itcna be desirable to set proper
     * hostname via this method
     *
     * @param specialHost
     * @throws MalformedURLException
     */
    public void setXmlRpcUrl(String specialHost) throws MalformedURLException {
        this.xmlRpcUrl = new URL("http://" + specialHost + ":" + realXPort + "/RPC2/");
    }

    /**
     * Default may lead to localhost, so itcna be desirable to set proper
     * hostname via this method
     *
     * @param specialHost
     * @throws MalformedURLException
     */
    public void setJekinsUrl(String specialHost) throws MalformedURLException {
        this.jenkinsUrlString = new URL("http://" + specialHost + ":" + realJPort + "/");
    }

    /**
     * @return the dbFileRoot
     */
    public File getDbFileRoot() {
        return dbFileRoot;
    }

    /**
     * @return the localReposRoot
     */
    public File getLocalReposRoot() {
        return localReposRoot;
    }

    /**
     * @return the realXPort
     */
    public int getRealXPort() {
        return realXPort;
    }

    /**
     * @return the realDPort
     */
    public int getRealDPort() {
        return realDPort;
    }

    /**
     * @return the realUPort
     */
    public int getRealUPort() {
        return realUPort;
    }

    /**
     * @return the previewPort
     */
    public int getPreviewPort() {
        return previewPort;
    }

    public URL getXmlRpcUrl() {
        return xmlRpcUrl;
    }

    public URL getJenkinsUrlString() {
        return jenkinsUrlString;
    }

    public URL getDownloadUrl() {
        return downloadUrl;
    }

}
