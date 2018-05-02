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
package org.fakekoji;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fakekoji.http.AccessibleSettings;
import org.fakekoji.http.PreviewFakeKoji;
import org.fakekoji.xmlrpc.server.KojiDownloadServer;
import org.fakekoji.xmlrpc.server.KojiXmlRpcServer;
import org.fakekoji.xmlrpc.server.ServerLogger;
import org.fakekoji.xmlrpc.server.SshApiService;

/**
 * This class is emulating selected xml-rpc calls to koji, so you can easily let
 * jenkins-scm-koji-plugin to connect to your own "database".
 *
 */
public class JavaServer {

    public static final int DEFAULT_JENKINS_PORT = 8080;
    public static final int DFAULT_RP2C_PORT = 9848;
    public static final int DFAULT_DWNLD_PORT = deductDwPort(DFAULT_RP2C_PORT);
    public static final int DFAULT_SSHUPLOAD_PORT = deductUpPort(DFAULT_RP2C_PORT);

    public static final String xPortAxiom = "XPORT";
    public static final String dPortAxiom = "DPORT";

    private final AccessibleSettings settings;

    KojiXmlRpcServer kojiXmlRpcServer;
    KojiDownloadServer kojiDownloadServer;
    PreviewFakeKoji previewFakeKojiServer;
    SshApiService sshApiServer;

    public JavaServer(AccessibleSettings settings) throws UnknownHostException, MalformedURLException {
        this.settings = settings;
        kojiXmlRpcServer = new KojiXmlRpcServer(settings);
        kojiDownloadServer = new KojiDownloadServer(settings.getDbFileRoot(), settings.getRealDPort());
        previewFakeKojiServer = new PreviewFakeKoji(settings);
        sshApiServer = new SshApiService(settings.getDbFileRoot(), settings.getRealUPort());
    }

    public void start() throws Exception {
        /* koji xmlRpc server*/
        ServerLogger.log("Attempting to start XML-RPC Server...");
        kojiXmlRpcServer.start();
        ServerLogger.log("Started successfully on " + kojiXmlRpcServer.getPort());
        /* koji download server*/
        ServerLogger.log("Starting http server to return files.");
        kojiDownloadServer.start();
        ServerLogger.log("Started successfully on " + kojiDownloadServer.getPort());
        /* preview (builds in human readable way) */
        ServerLogger.log("Starting http server frontend with settings answers");
        previewFakeKojiServer.start();
        ServerLogger.log("FrontEnd started successfully on " + previewFakeKojiServer.getPort());
        /* ssh server to upload files to fakekoji */
        ServerLogger.log("Starting sshd server to accept files.");
        sshApiServer.start();
        ServerLogger.log("Sshd server started successfully on " + +sshApiServer.getPort());
    }

    public void stop() {
        try {
            sshApiServer.stop();
        } catch (IOException ex) {
            Logger.getLogger(JavaServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        previewFakeKojiServer.stop();
        kojiDownloadServer.stop();
        kojiXmlRpcServer.stop();
    }

    private static final String XMLRPC_SWITCH = "-xmlrpcport";
    private static final String DOWNLOAD_SWITCH = "-downloadport";
    private static final String SSH_UPLOAD_SWITCH = "-sshuploadport";
    private static final String VIEW1_SWITCH = "-view1port";
    private static final String JENKINS_SWITCH = "-jenkinsport";
    private static final String REPOS_SWITCH = "-reposhome";
    private static final String DB_SWITCH = "-kojidbhome";
    private static final String JENKINS_HOST_NAME_SWITCH = "-jenkinshost";

    public static void main(String[] args) throws Exception {
        int realXPort = DFAULT_RP2C_PORT;
        int realDPort = DFAULT_DWNLD_PORT;
        int realUPort = DFAULT_SSHUPLOAD_PORT;
        int realJport = DEFAULT_JENKINS_PORT;
        String hostname = null;
        int view1Port = 80;
        File dbFileRoot = null;
        File reposFileRoot = null;

        //to deduct or not to deduct? Alias how to enforce user to set them all :-)
        boolean wasDport = false;
        boolean wasUport = false;
        //to enforce those two
        boolean wasRepos = false;
        boolean wasDbRoot = false;

        //args = new String[]{DB_SWITCH + "=/home/jvanek/Desktop", REPOS_SWITCH + "=/home/jvanek/hg", VIEW1_SWITCH+"=8888"};
        //args = new String[]{DB_SWITCH + "=/home/jvanek/Desktop", REPOS_SWITCH + "=/home/jvanek/hg", VIEW1_SWITCH+"=8888", JENKINS_HOST_NAME_SWITCH+"=hydra.brq.redhat.com"};
        if (args.length < 2) {
            throw new RuntimeException("\n"
                    + "expected two mandatory switches: " + DB_SWITCH + "=/path/to/koji/like/db and " + REPOS_SWITCH + "=/path/to/repos/with/clones\n"
                    + "Then you can use any of " + VIEW1_SWITCH + ", " + XMLRPC_SWITCH + ", " + DOWNLOAD_SWITCH + ", " + SSH_UPLOAD_SWITCH + " (-switch=value syntax) to overwrite defaults:\n"
                    + "xml rpc have defaulr of " + DFAULT_RP2C_PORT + "\n"
                    + "download port have default of " + DFAULT_DWNLD_PORT + " (deducted from XML_RPC by +1)\n"
                    + "sshd port (have defualt defualt of " + DFAULT_SSHUPLOAD_PORT + "( XML_RPC port -26)\n"
                    + "view1 have default of " + view1Port + "\n"
                    + "jenkins have default of " + realJport + "\n"
                    + "you may change jenkins hostname of this runtime via " + JENKINS_HOST_NAME_SWITCH + "\n");
        }
        for (String arg : args) {
            String key = arg.split("=")[0];
            String value = arg.split("=")[1];
            key = key.replaceAll("^-+", "-");
            switch (key) {
                case (DB_SWITCH):
                    dbFileRoot = new File(value);
                    wasDbRoot = true;
                    break;
                case (REPOS_SWITCH):
                    reposFileRoot = new File(value);
                    wasRepos = true;
                    break;
                case (JENKINS_HOST_NAME_SWITCH):
                    hostname = value;
                    break;
                case (XMLRPC_SWITCH):
                    realXPort = Integer.valueOf(value);
                    break;
                case (DOWNLOAD_SWITCH):
                    realDPort = Integer.valueOf(value);
                    wasDport = true;
                    break;
                case (SSH_UPLOAD_SWITCH):
                    realUPort = Integer.valueOf(value);
                    wasUport = true;
                    break;
                case (VIEW1_SWITCH):
                    view1Port = Integer.valueOf(value);
                    break;
                case (JENKINS_SWITCH):
                    realJport = Integer.valueOf(value);
                    break;
                default:
                    throw new RuntimeException("Unknown argument in token: " + arg);
            }
        }
        if (!wasDbRoot) {
            throw new RuntimeException(DB_SWITCH + " and " + REPOS_SWITCH + " are mandatory");
        }
        if (!wasRepos) {
            throw new RuntimeException(DB_SWITCH + " and " + REPOS_SWITCH + " are mandatory");
        }

        if (!wasDport) {
            realDPort = deductDwPort(realXPort);
        }
        if (!wasUport) {
            realUPort = deductUpPort(realXPort);
        }
        AccessibleSettings settings = new AccessibleSettings(dbFileRoot, reposFileRoot, realXPort, realDPort, realUPort, view1Port, realJport);
        if (hostname != null) {
            settings.setJekinsUrl(hostname);
        }
        JavaServer javaServer = new JavaServer(settings);

        javaServer.start();

    }

    public static int deductDwPort(int xport) {
        return xport + 1;
    }

    public static int deductUpPort(int xport) {
        return xport - 26;
    }
}
