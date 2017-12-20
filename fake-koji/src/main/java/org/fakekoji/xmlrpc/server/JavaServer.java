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

import com.sun.net.httpserver.HttpServer;
import hudson.plugins.scm.koji.Constants;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.webserver.WebServer;
import org.fakekoji.http.PreviewFakeKoji;
import org.fakekoji.xmlrpc.server.core.FileReturningHandler;

/**
 * This class is emulating selected xml-rpc calls to koji, so you can easily let
 * jenkins-scm-koji-plugin to connect to your own "database".
 *
 */
public class JavaServer {

    private static File dbFileRoot;
    public static final int DFAULT_RP2C_PORT = 9848;
    public static final int DFAULT_DWNLD_PORT = deductDwPort(DFAULT_RP2C_PORT);
    public static final int DFAULT_SSHUPLOAD_PORT = deductUpPort(DFAULT_RP2C_PORT);
    private static int realXPort;
    private static int realDPort;
    private static int realUPort;

    public static final String xPortAxiom = "XPORT";
    public static final String dPortAxiom = "DPORT";

    /**
     * Testing method (see JavaClient) to verify if server works at all.
     *
     * @param x
     * @param y
     * @return x+y
     */
    private Integer sum(int x, int y) {
        return x + y;
    }

    /**
     * xmlrpc api for sum.
     *
     * @param x
     * @param y
     * @return x+y
     */
    public Integer sum(Object x, Object y) {
        return sum(((Integer) x).intValue(), ((Integer) y).intValue());
    }

    public static void main(String[] args) {
        realXPort = DFAULT_RP2C_PORT;
        realDPort = DFAULT_DWNLD_PORT;
        realUPort = DFAULT_SSHUPLOAD_PORT;
        //args=new String[]{"/mnt/raid1/local-builds"};
        if (args.length < 1) {
            throw new RuntimeException("expected at least one argument - directory with koji-like \"database\".\n"
                    + "second is optional xml-rpcport port (then download port is deducted by +1).\n"
                    + "third is optional download port.\n"
                    + "fourth is optional sshd port (otherwise defualt (or set xmlrpc) port -26 (default 9822).\n");
        } else {
            dbFileRoot = new File(args[0]);
            System.out.println("testing koji-like databse " + dbFileRoot.getAbsolutePath());
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
        try {

            System.out.println("Attempting to start XML-RPC Server...");

            WebServer server = new WebServer(realXPort);
            server.setParanoid(false);
            XmlRpcHandlerMapping xxx = new XmlRpcHandlerMapping() {
                @Override
                /**
                 * Very naive implementation of xml-rpc handler with hardcoded
                 * calls
                 */
                public XmlRpcHandler getHandler(String string) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                    return new XmlRpcHandler() {
                        @Override
                        public Object execute(XmlRpcRequest xrr) throws XmlRpcException {
                            System.out.println(new Date().toString() + " Requested: " + xrr.getMethodName());
                            //need reinitializzing, as new  build couldbe added
                            FakeKojiDB kojiDb = new FakeKojiDB(dbFileRoot);
                            if (xrr.getMethodName().equals("sample.sum")) {
                                //testing method
                                return new JavaServer().sum(xrr.getParameter(0), xrr.getParameter(1));
                            }
                            if (xrr.getMethodName().equals(Constants.getPackageID)) {
                                //input is package name, eg java-1.8.0-openjdk
                                //result is int, package id in koji database
                                //so this impl must have internal "database" (eg itw do not have number in name)
                                return kojiDb.getPkgId(xrr.getParameter(0).toString());
                            }
                            if (xrr.getMethodName().equals(Constants.listBuilds)) {
                                //inut is hashmap with
                                /* paramsMap.put(packageID, packageId);
                                 paramsMap.put("state", 1);
                                 paramsMap.put("__starstar", Boolean.TRUE
                                 */
                                // from those we care only about packageID
                                return kojiDb.getProjectBuildsByProjectIdAsMaps(((Integer) ((Map) (xrr.getParameter(0))).get(Constants.packageID)));
                                //return is array of objects,  where each meber ishash map
                                //koji plugin seems to  care about version, release and build_id (so each build needs some int hash:( tos erve as "database"
                                //later maybe date, name, nvr, arch, completion_time, rpms, tags, 
                            }
                            if (xrr.getMethodName().equals(Constants.listTags)) {
                                // input is buildId
                                // output object[] wehre mamabers are hashmaps, where we care baout "name" only
                                return kojiDb.getTags(((Map) (xrr.getParameter(0))));
                            }

                            if (xrr.getMethodName().equals(Constants.listRPMs)) {
                                //input is hashmap buildID->integer, arches->String[] and uninteresed __starstar->true
                                //output is array off hashmaps
                                return kojiDb.getRpms(((Map) (xrr.getParameter(0))).get(Constants.buildID), ((Map) (xrr.getParameter(0))).get(Constants.arches));
                            }
                            if (xrr.getMethodName().equals(Constants.listArchives)) {
                                //input is hashmap buildID->integer, arches->String[] and uninteresed __starstar->true
                                //output is array off hashmaps
                                return kojiDb.getArchives(((Map) (xrr.getParameter(0))).get(Constants.buildID), ((Map) (xrr.getParameter(0))).get(Constants.arches));
                            }
                            return null;
                        }
                    };
                }
            };
            server.getXmlRpcServer().setHandlerMapping(xxx);
            //server.addHandler("sample", new JavaServer());
            server.start();
            System.out.println("Started successfully on " + realXPort);
            System.out.println("Starting http server to return files.");
            HttpServer hs = HttpServer.create(new InetSocketAddress(realDPort), 0);
            hs.createContext("/", new FileReturningHandler(dbFileRoot));
            hs.start();
            System.out.println("Started successfully on " + realDPort);
            System.out.println("Starting http server 80 frontend");
            //This is nasty, this now requires whole service run as root. Should be fixed  to two separated serv
            String thisMachine = InetAddress.getLocalHost().getHostName();
//            PreviewFakeKoji.main(new String[]{
//                "http://" + thisMachine + "/RPC2/",
//                "http://" + thisMachine + "/",
//                new File(dbFileRoot.getParentFile(), "upstream-repos").getAbsolutePath(),
//                dbFileRoot.getAbsolutePath()
//            });
            PreviewFakeKoji.setJenkinsUrlOverride("http://hydra.brq.redhat.com:8080/");
            System.out.println("FrontEnd started successfully");
            System.out.println("Starting sshd server to accept files.");
            new SshUploadService().setup(realUPort, dbFileRoot);
            System.out.println("Started successfully on " + realUPort);
            System.out.println("Accepting requests. (Halt program to stop.)");

        } catch (Exception exception) {
            System.err.println("JavaServer: " + exception);
            exception.printStackTrace();
        }
    }

    public static int deductDwPort(int xport) {
        return xport + 1;
    }

    public static int deductUpPort(int xport) {
        return xport - 26;
    }
}
