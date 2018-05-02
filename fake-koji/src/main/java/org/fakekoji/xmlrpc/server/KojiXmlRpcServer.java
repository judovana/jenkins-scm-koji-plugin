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
package org.fakekoji.xmlrpc.server;

import hudson.plugins.scm.koji.Constants;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.fakekoji.http.AccessibleSettings;

/**
 * This Server implements Koji XmlRpc API (It is called by jenkins koji plugin )
 *
 * Based on code, which was originally in JavaServer class.
 */
public class KojiXmlRpcServer {

    private WebServer webServer;
    AccessibleSettings settings;

    public KojiXmlRpcServer(AccessibleSettings settings) {
        this.settings = settings;
        this.webServer = new WebServer(settings.getRealXPort());
    }

    /**
     * Testing method (see JavaClient) to verify if server works at all.
     *
     * @param x
     * @param y
     * @return x+y
     */
    private static Integer sum(int x, int y) {
        return x + y;
    }

    /**
     * xmlrpc api for sum.
     *
     * @param x
     * @param y
     * @return x+y
     */
    public static Integer sum(Object x, Object y) {
        return sum(((Integer) x).intValue(), ((Integer) y).intValue());
    }

    public int getPort() {
        return settings.getRealXPort();
    }

    public void start() throws IOException {
        webServer = new WebServer(settings.getRealXPort());
        webServer.setParanoid(false);
        XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
        config.setEnabledForExtensions(true);
        webServer.getXmlRpcServer().setConfig(config);

        XmlRpcHandlerMapping xxx = new XmlRpcHandlerMapping() {
            @Override
            /**
             * Very naive implementation of xml-rpc handler with hardcoded calls
             */
            public XmlRpcHandler getHandler(String string) throws XmlRpcNoSuchHandlerException, XmlRpcException {
                return new XmlRpcHandler() {
                    @Override
                    public Object execute(XmlRpcRequest xrr) throws XmlRpcException {
                        ServerLogger.log(new Date().toString() + " Requested: " + xrr.getMethodName());
                        //need reinitializzing, as new  build couldbe added
                        FakeKojiDB kojiDb = new FakeKojiDB(settings);
                        if (xrr.getMethodName().equals("sample.sum")) {
                            //testing method
                            return sum(xrr.getParameter(0), xrr.getParameter(1));
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
        webServer.getXmlRpcServer().setHandlerMapping(xxx);
        //server.addHandler("sample", new JavaServer());
        webServer.start();
    }

    public void stop() {
        webServer.shutdown();
    }

}
