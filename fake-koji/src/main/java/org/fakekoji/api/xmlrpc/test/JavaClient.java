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
package org.fakekoji.api.xmlrpc.test;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.client.tools.XmlRpcHelper;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;

import hudson.plugins.scm.koji.model.Build;
import org.apache.xmlrpc.XmlRpcException;
import org.fakekoji.server.JavaServer;
import org.fakekoji.xmlrpc.server.XmlRpcRequestParams;
import org.fakekoji.xmlrpc.server.XmlRpcRequestParamsBuilder;
import org.fakekoji.xmlrpc.server.XmlRpcResponse;

/**
 * This class is simple test which connects to running
 * org.fakekoji.xmlrpc.server.JavaServer and try to invoke testing method for
 * summ of two nubers. This class is for testing connection only
 *
 */
public class JavaClient {

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
        final XmlRpcRequestParamsBuilder paramsBuilderFirst = new XmlRpcRequestParamsBuilder();
        paramsBuilderFirst.setPackageName("java-1.8.0-openjdk");
        final XmlRpcRequestParams paramsFirst = paramsBuilderFirst.build();
        final XmlRpcResponse responseFirst = execute(Constants.getPackageID, paramsFirst);
        final Integer packageId = responseFirst.getPackageId();
        System.out.println(packageId);

        final XmlRpcRequestParamsBuilder paramsBuilderSecond = new XmlRpcRequestParamsBuilder();
        paramsBuilderSecond.setPackageId(packageId);
        paramsBuilderSecond.setState(1);
        paramsBuilderSecond.setStarstar(Boolean.TRUE);
        final XmlRpcRequestParams paramsSecond = paramsBuilderSecond.build();

        final XmlRpcResponse responseSecond = execute(Constants.listBuilds, paramsSecond);
        List<Build> builds = responseSecond.getBuilds();
        System.out.println(builds.size());
        for (Build build : builds) {

            final Integer buildId = build.getId();
            System.out.println(buildId);
            System.out.println(build.getNvr());

            final XmlRpcRequestParamsBuilder paramsBuilderThird = new XmlRpcRequestParamsBuilder();
            paramsBuilderThird.setBuildId(buildId);
            paramsBuilderThird.setStarstar(Boolean.TRUE);
            final XmlRpcRequestParams paramsThird = paramsBuilderThird.build();
            XmlRpcResponse responseThird = execute(Constants.listTags, paramsThird);
            Set<String> tags = responseThird.getTags();
            System.out.println("  " + tags.size());
            for (String tag : tags) {
                System.out.println("   " + tag);
            }

        }

    }

    protected static XmlRpcResponse execute(String methodName, XmlRpcRequestParams params) {
        return new XmlRpcHelper.XmlRpcExecutioner("http://hydra.brq.redhat.com:" + JavaServer.DFAULT_RP2C_PORT + "/RPC2").execute(methodName, params);
    }
}
