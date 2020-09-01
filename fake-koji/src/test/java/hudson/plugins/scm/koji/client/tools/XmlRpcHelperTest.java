/*
 * The MIT License
 *
 * Copyright 2018 jvanek.
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
package hudson.plugins.scm.koji.client.tools;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.junit.Test;
import org.apache.xmlrpc.webserver.WebServer;
import org.junit.Assert;

public class XmlRpcHelperTest {

    public static class TimeoutingXmlRpcServer {

        private final int port;
        private final WebServer webServer;
        private final int delay;

        public TimeoutingXmlRpcServer(int port, int holdOn) {
            this.port = port;
            this.delay = holdOn;
            this.webServer = new WebServer(port);
        }

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
            return port;
        }

        public void start() throws IOException {
            XmlRpcServerConfigImpl config = new XmlRpcServerConfigImpl();
            config.setEnabledForExtensions(true);
            webServer.getXmlRpcServer().setConfig(config);
            XmlRpcHandlerMapping xxx = (String string) -> new XmlRpcHandler() {
                @Override
                public Object execute(XmlRpcRequest xrr) throws XmlRpcException {
                    pretendLoad();
                    if (xrr.getMethodName().equals(SummXmlRpcParam.SUMM_METHOD)) {
                        //testing method
                        return sum(xrr.getParameter(0), xrr.getParameter(1));
                    }
                    return null;
                }

                private void pretendLoad() {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
            webServer.getXmlRpcServer().setHandlerMapping(xxx);
            webServer.start();
        }

        public void stop() {
            webServer.shutdown();
        }

    }


    private static class SummXmlRpcParam implements XmlRpcRequestParams {
        private static final String SUMM_METHOD = "sample.sum";
        ;
        private final int a1;
        private final int a2;

        public SummXmlRpcParam(int a1, int a2) {
            this.a1 = a1;
            this.a2 = a2;
        }

        @Override
        public Object[] toXmlRpcParams() {
            return new Object[]{a1, a2};
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SummXmlRpcParam that = (SummXmlRpcParam) o;
            return a1 == that.a1 &&
                    a2 == that.a2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(a1, a2);
        }

        @Override
        public String getMethodName() {
            return SUMM_METHOD;
        }

    }

    private final SummXmlRpcParam defaultSumHelper = new SummXmlRpcParam(3, 5);

    @Test
    public void tryConnectionWorks() throws IOException {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }
        Object result;
        TimeoutingXmlRpcServer w = new TimeoutingXmlRpcServer(port, 1);
        XmlRpcHelper.XmlRpcExecutioner a = new XmlRpcHelper.XmlRpcExecutioner("http://localhost:" + port + "/RPC2/");
        w.start();
        try {
            result = a.execute(defaultSumHelper);
        } finally {
            w.stop();
        }
        Assert.assertEquals(8, result);

    }

    @Test
    public void tryConnectionNotTimeouts() throws IOException {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }
        Object result;
        TimeoutingXmlRpcServer w = new TimeoutingXmlRpcServer(port, 1000);
        XmlRpcHelper.XmlRpcExecutioner a = new XmlRpcHelper.XmlRpcExecutioner("http://localhost:" + port + "/RPC2/");
        a.setTimeout(2000);
        w.start();
        try {
            result = a.execute(defaultSumHelper);
        } finally {
            w.stop();
        }
        Assert.assertEquals(8, result);
    }

    @Test
    public void tryConnectionTimeouts() throws IOException {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            port = serverSocket.getLocalPort();
        }
        Object result = null;
        Exception thrown = null;
        TimeoutingXmlRpcServer w = new TimeoutingXmlRpcServer(port, 2000);
        XmlRpcHelper.XmlRpcExecutioner a = new XmlRpcHelper.XmlRpcExecutioner("http://localhost:" + port + "/RPC2/");
        a.setTimeout(1000);
        w.start();
        try {
            result = a.execute(defaultSumHelper);
        } catch (Exception ex) {
            thrown = ex;
        } finally {
            w.stop();
        }
        Assert.assertNull(result);
        Assert.assertNotNull(thrown);
    }


}
