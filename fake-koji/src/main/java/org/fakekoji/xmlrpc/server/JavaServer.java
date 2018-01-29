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
package org.fakekoji.xmlrpc.server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fakekoji.http.PreviewFakeKoji;

/**
 * This class is emulating selected xml-rpc calls to koji, so you can easily let
 * jenkins-scm-koji-plugin to connect to your own "database".
 *
 */
public class JavaServer {

    public static final int DFAULT_RP2C_PORT = 9848;
    public static final int DFAULT_DWNLD_PORT = deductDwPort(DFAULT_RP2C_PORT);
    public static final int DFAULT_SSHUPLOAD_PORT = deductUpPort(DFAULT_RP2C_PORT);

    public static final String xPortAxiom = "XPORT";
    public static final String dPortAxiom = "DPORT";

    private File dbFileRoot;
    private File localReposRoot;

    private int realXPort;
    private int realDPort;
    private int realUPort;
    private int previewPort;

    KojiXmlRpcServer kojiXmlRpcServer;
    KojiDownloadServer kojiDownloadServer;
    PreviewFakeKoji previewFakeKojiServer;
    SshApiService sshApiServer;

    public JavaServer(File dbFileRoot, File localReposRoot,
            int realXPort, int realDPort, int realUPort, int previewPort) {
        this.dbFileRoot = dbFileRoot;
        this.localReposRoot = localReposRoot;

        this.realXPort = realXPort;
        this.realDPort = realDPort;
        this.realUPort = realUPort;
        this.previewPort = previewPort;

        kojiXmlRpcServer = new KojiXmlRpcServer(dbFileRoot, realXPort);
        kojiDownloadServer = new KojiDownloadServer(dbFileRoot, realDPort);

        String thisMachine;
        URL xmlRpcUrl;
        URL downloadUrl;

        try {
            thisMachine = InetAddress.getLocalHost().getHostName();
            xmlRpcUrl = new URL("http://" + thisMachine + "/RPC2/");
            downloadUrl = new URL("http://" + thisMachine + "/");

            previewFakeKojiServer = new PreviewFakeKoji(
                    xmlRpcUrl,
                    downloadUrl,
                    localReposRoot,
                    dbFileRoot,
                    previewPort,
                    "http://hydra.brq.redhat.com:8080/");

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        sshApiServer = new SshApiService(dbFileRoot, realUPort);
    }

    public void start() throws Exception {
        /* koji xmlRpc server*/
        ServerLogger.log("Attempting to start XML-RPC Server...");
        kojiXmlRpcServer.start();
        ServerLogger.log("Started successfully on " + realXPort);
        /* koji download server*/
        ServerLogger.log("Starting http server to return files.");
        kojiDownloadServer.start();
        ServerLogger.log("Started successfully on " + realDPort);
        /* preview (builds in human readable way) */
        ServerLogger.log("Starting http server frontend on " + previewPort);
        previewFakeKojiServer.start();
        ServerLogger.log("FrontEnd started successfully");
        /* ssh server to upload files to fakekoji */
        ServerLogger.log("Starting sshd server to accept files.");
        sshApiServer.start();
        ServerLogger.log("Sshd server started successfully on " + realUPort);
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

    public static void main(String[] args) throws Exception {
        int realXPort = DFAULT_RP2C_PORT;
        int realDPort = DFAULT_DWNLD_PORT;
        int realUPort = DFAULT_SSHUPLOAD_PORT;
        File dbFileRoot;
        //args=new String[]{"/mnt/raid1/local-builds"};
        if (args.length < 1) {
            throw new RuntimeException("expected at least one argument - directory with koji-like \"database\".\n"
                    + "second is optional xml-rpcport port (then download port is deducted by +1).\n"
                    + "third is optional download port.\n"
                    + "fourth is optional sshd port (otherwise defualt (or set xmlrpc) port -26 (default 9822).\n");
        } else {
            dbFileRoot = new File(args[0]);
            ServerLogger.log("testing koji-like databse " + dbFileRoot.getAbsolutePath());
            //cache FS
            FakeKojiDB test = new FakeKojiDB(dbFileRoot);
            test.checkAll();
        }
        if (args.length == 2) {
            realXPort = Integer.valueOf(args[1]);
            realDPort = deductDwPort(realXPort);
            realUPort = deductUpPort(realXPort);
        }
        if (args.length == 3) {
            realXPort = Integer.valueOf(args[1]);
            realDPort = Integer.valueOf(args[2]);
            realUPort = deductUpPort(realXPort);
        }
        if (args.length == 4) {
            realXPort = Integer.valueOf(args[1]);
            realDPort = Integer.valueOf(args[2]);
            realUPort = Integer.valueOf(args[3]);
        }
        JavaServer javaServer = new JavaServer(dbFileRoot, new File(dbFileRoot.getParentFile(), "upstream-repos"), realXPort, realDPort, realUPort, 80);

        javaServer.start();

    }

    public static int deductDwPort(int xport) {
        return xport + 1;
    }

    public static int deductUpPort(int xport) {
        return xport - 26;
    }
}
