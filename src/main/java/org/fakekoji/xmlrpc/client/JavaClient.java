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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.fakekoji.xmlrpc.server.JavaServer;

/**
 * This class is simple test which connects to running
 * org.fakekoji.xmlrpc.server.JavaServer and try to invoke testing method for
 * summ of two nubers. This class is for testing connection only
 *
 */
public class JavaClient {

    public static void main(String[] args) throws MalformedURLException, XmlRpcException {

        XmlRpcClientConfigImpl xmlRpcConfig = new XmlRpcClientConfigImpl();
        xmlRpcConfig.setServerURL(new URL("http://localhost:" + JavaServer.DFAULT_RP2C_PORT + "/RPC2"));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(xmlRpcConfig);

        List params = new ArrayList();

        params.add(17);
        params.add(13);

        Object result = client.execute("sample.sum", params);

        int sum = ((Integer) result);
        System.out.println("The sum is: " + sum);

    }
}
