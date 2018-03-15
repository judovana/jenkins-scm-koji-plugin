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
package org.fakekoji.xmlrpc.client;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.client.tools.XmlRpcHelper;
import java.net.MalformedURLException;
import java.util.*;
import org.apache.xmlrpc.XmlRpcException;
import org.fakekoji.JavaServer;

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
        Integer packageId = (Integer) execute(Constants.getPackageID, "java-1.8.0-openjdk");
        System.out.println(packageId);
        Map listBuildsparamsMap = new HashMap();
        listBuildsparamsMap.put(Constants.packageID, packageId);
        listBuildsparamsMap.put("state", 1);
        listBuildsparamsMap.put("__starstar", Boolean.TRUE);
        Object[] results = (Object[]) execute(Constants.listBuilds, listBuildsparamsMap);
        System.out.println(results.length);
        for (Object result : results) {
            Map m = (Map) result;

            Map listTagsParamsMap = new HashMap();
            Object bId = m.get(Constants.build_id);
            System.out.println(bId);
            System.out.println(m.get(Constants.nvr));
            listTagsParamsMap.put(Constants.build, bId);
            listTagsParamsMap.put("__starstar", Boolean.TRUE);
            Object[] res = (Object[]) execute(Constants.listTags, listTagsParamsMap);
            System.out.println("  " + res.length);
            for (Object re : res) {
                Map mm = (Map) re;
                Object tag = mm.get(Constants.name);
                System.out.println("   " + tag);
            }

        }

    }

    protected static Object execute(String methodName, Object... args) {
        return new XmlRpcHelper.XmlRpcExecutioner("http://hydra.brq.redhat.com:" + JavaServer.DFAULT_RP2C_PORT + "/RPC2").execute(methodName, args);
    }
}
