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

import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
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

        XmlRpcHandlerMapping xxx = string -> xmlRpcRequest -> {
            ServerLogger.log(new Date().toString() + " Requested: " + xmlRpcRequest.getMethodName());
            //need reinitializzing, as new  build could be added
            FakeKojiDB kojiDb = new FakeKojiDB(settings);
            if (xmlRpcRequest.getMethodName().equals("sample.sum")) {
                //testing method
                return sum(xmlRpcRequest.getParameter(0), xmlRpcRequest.getParameter(1));
            }
            XmlRpcRequestParamsBuilder paramsBuilder = new XmlRpcRequestParamsBuilder();

            final String methodName = xmlRpcRequest.getMethodName();
            XmlRpcRequestParams params = paramsBuilder.build(xmlRpcRequest.getParameter(0), methodName);

            final XmlRpcResponseBuilder responseBuilder = new XmlRpcResponseBuilder();
            switch (methodName) {
                case Constants.getPackageID:
                    responseBuilder.setPackageId(kojiDb.getPkgId(params.getPackageName()));
                    break;
                case Constants.listBuilds:
                    responseBuilder.setBuilds(kojiDb.getProjectBuilds(params.getPackageId()));
                    break;
                case Constants.listTags:
                    responseBuilder.setTags(kojiDb.getTags(params.getBuildId()));
                    break;
                case Constants.listRPMs:
                    responseBuilder.setRpms(kojiDb.getRpms(params.getBuildId(), params.getArchs()));
                    break;
                case Constants.listArchives:
                    responseBuilder.setArchives(kojiDb.getArchives(params.getBuildId(), params.getArchs()));
                    break;
            }
            return responseBuilder.build().toObject();
        };
        webServer.getXmlRpcServer().setHandlerMapping(xxx);
        //server.addHandler("sample", new JavaServer());
        webServer.start();
    }

    public void stop() {
        webServer.shutdown();
    }

}
