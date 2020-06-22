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
package org.fakekoji.api.xmlrpc;

import hudson.plugins.scm.koji.client.tools.XmlRpcHelper;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;
import org.apache.xmlrpc.XmlRpcException;
import org.fakekoji.core.AccessibleSettings;
import org.fakekoji.server.JavaServer;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.GetPackageId;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListBuilds;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListRPMs;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.ListTags;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.BuildList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.PackageId;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.RPMList;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.TagSet;
import org.fakekoji.xmlrpc.server.xmlrpcresponse.XmlRpcResponse;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * This class is simple test which connects to running
 * org.fakekoji.xmlrpc.server.JavaServer and try to invoke testing method for
 * summ of two nubers. This class is for testing connection only
 *
 */
public class JavaTestClient {

    public static void main(String[] args) throws MalformedURLException, XmlRpcException {

//        XmlRpcClientConfigImpl xmlRpcConfig = new XmlRpcClientConfigImpl();
//        xmlRpcConfig.setServerURL(new URL("http://localhost:" + JavaServer.DFAULT_RP2C_PORT + "/RPC2"));
//        xmlRpcConfig.setServerURL(new URL("http://hydra.brq.redhat.com:" + JavaServer.DFAULT_RP2C_PORT + "/RPC2"));
//       XmlRpcClient client = new XmlRpcClient();
//        client.setConfig(xmlRpcConfig);
//        List params = new ArrayList();
//        params.add(17);
//        params.add(13);
//
//        Object result = client.execute("sample.sum", params);
//        int sum = ((Integer) result);
//        System.out.println("The sum is: " + sum);

        final long timeThen = System.nanoTime();
        final String packageName = "java-1.8.0-openjdk";

        System.out.println("Getting ID of package: " + packageName);
        final XmlRpcRequestParams getPackageIdParams = new GetPackageId(packageName);
        final PackageId packageIdResponse = PackageId.create(execute(getPackageIdParams));
        final Integer packageId = packageIdResponse.getValue();
        System.out.println("ID of package: " + packageName + ": " + packageId);

        final XmlRpcRequestParams listBuildsParams = new ListBuilds(packageId);

        System.out.println("Getting builds of package: " + packageName);
        final BuildList buildListResponse = BuildList.create(execute(listBuildsParams));
        List<Build> builds = buildListResponse.getValue();
        System.out.println("Number of builds: " + builds.size());
        for (Build build : builds) {

            final Integer buildId = build.getId();
            System.out.println("Build " + build.getNvr() + " (" + buildId + ')');
            System.out.println();

            final XmlRpcRequestParams listTagsParams = new ListTags(buildId);
            TagSet tagSetResponse = TagSet.create(execute(listTagsParams));
            Set<String> tags = tagSetResponse.getValue();
            System.out.println("  tags(" + tags.size() + "):");
            for (String tag : tags) {
                System.out.println("   " + tag);
            }
            final XmlRpcRequestParams listRPMsParams = new ListRPMs(
                    buildId,
                    Arrays.asList("x86_64", "i686")
            );
            final RPMList rpmList = RPMList.create(execute(listRPMsParams));
            final List<RPM> rpms = rpmList.getValue();
            System.out.println("  rpms(" + rpms.size() + "):");
            for (RPM rpm : rpms) {
                System.out.println("     Filename: " + rpm.getFilename(".tarxz"));
                System.out.println("     Hash sum: " + rpm.getHashSum());
                System.out.println("     NVR: " + rpm.getNvr());
                System.out.println("     Name: " + rpm.getName());
                System.out.println("     Version: " + rpm.getVersion());
                System.out.println("     Release: " + rpm.getRelease());
                System.out.println("     Arch: " + rpm.getArch());
                System.out.println("     Url: " + rpm.getUrl());
                System.out.println();
            }
        }
        final long timeNow = System.nanoTime();
        System.out.println(timeNow - timeThen);
    }

    protected static Object execute(XmlRpcRequestParams params) {
        return new XmlRpcHelper.XmlRpcExecutioner(AccessibleSettings.master.baseUrl+":" + JavaServer.DEFAULT_XML_RPC_PORT + "/RPC2").execute(params);
    }
}
