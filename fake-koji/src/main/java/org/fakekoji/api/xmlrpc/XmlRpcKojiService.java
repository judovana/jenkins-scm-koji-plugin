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
package org.fakekoji.api.xmlrpc;

import hudson.plugins.scm.koji.Constants;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.core.FakeKojiDB;
import org.fakekoji.xmlrpc.server.JavaServerConstants;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetPackageId;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListArchives;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListBuilds;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListRPMs;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListTags;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.ArchiveList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.BuildList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.PackageId;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.RPMList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.TagSet;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.XmlRpcResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * This Server implements Koji XmlRpc API (It is called by jenkins koji plugin )
 *
 * Based on code, which was originally in JavaServer class.
 */
public class XmlRpcKojiService {

    private static final Logger LOGGER = Logger.getLogger(JavaServerConstants.FAKE_KOJI_LOGGER);

    private WebServer webServer;
    AccessibleSettings settings;

    public XmlRpcKojiService(AccessibleSettings settings) {
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
            LOGGER.info("Requested: " + xmlRpcRequest.getMethodName());
            //need reinitializzing, as new  build could be added
            FakeKojiDB kojiDb = new FakeKojiDB(settings);
            if (xmlRpcRequest.getMethodName().equals("sample.sum")) {
                //testing method
                return sum(xmlRpcRequest.getParameter(0), xmlRpcRequest.getParameter(1));
            }
            final Object parameter = xmlRpcRequest.getParameter(0);

            final XmlRpcResponse response;
            switch (xmlRpcRequest.getMethodName()) {
                case Constants.getPackageID:
                    response = new PackageId(kojiDb.getPkgId(GetPackageId.create(parameter).getPackageName()));
                    break;
                case Constants.listBuilds:
                    response = new BuildList(kojiDb.getProjectBuilds(ListBuilds.create(parameter).getPackageId()));
                    break;
                case Constants.listTags:
                    response = new TagSet(kojiDb.getTags(ListTags.create(parameter).getBuildId()));
                    break;
                case Constants.listRPMs:
                    final ListRPMs listRPMsParams = ListRPMs.create(parameter);
                    response = new RPMList(kojiDb.getRpms(listRPMsParams.getBuildId(), listRPMsParams.getArchs()));
                    break;
                case Constants.listArchives:
                    final ListArchives listArchivesParams = ListArchives.create(parameter);
                    response = new ArchiveList(kojiDb.getArchives(listArchivesParams.getBuildId(), listArchivesParams.getArchs()));
                    break;
                default:
                    return null;
            }
            return response.toObject();
        };
        webServer.getXmlRpcServer().setHandlerMapping(xxx);
        //server.addHandler("sample", new JavaServer());
        webServer.start();
    }

    public void stop() {
        webServer.shutdown();
    }

}
