/*
 * The MIT License
 *
 * Copyright 2016 jvanek.
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

import java.net.URL;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.AtomicParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.I4Serializer;
import org.apache.xmlrpc.serializer.I8Serializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author jvanek
 */
public class XmlRpcHelper {

    public static class XmlRpcExecutioner {

        private final String currentURL ;
        private Integer timeout = 60*1000;

        public XmlRpcExecutioner(String currentURL) {
            this.currentURL = currentURL;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Object execute(XmlRpcRequestParams params) {
            try {
                final XmlRpcClient client = createClient();
                return client.execute(params.getMethodName(), params.toXmlRpcParams());
            } catch (Exception ex) {
                throw new RuntimeException("Exception while executing " + params.getMethodName(), ex);
            }
        }

        private XmlRpcClient createClient() throws Exception {
            XmlRpcClientConfigImpl xmlRpcConfig = new XmlRpcClientConfigImpl();
            xmlRpcConfig.setEnabledForExtensions(true);
            xmlRpcConfig.setServerURL(new URL(currentURL));
            if (timeout != null) {
                xmlRpcConfig.setConnectionTimeout(timeout);
                xmlRpcConfig.setReplyTimeout(timeout);
                xmlRpcConfig.setEnabledForExtensions(true);
            }
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(xmlRpcConfig);
            client.setTypeFactory(new KojiTypeFactory(client));
            return client;
        }

    }

    private static class KojiTypeFactory extends TypeFactoryImpl {

        public KojiTypeFactory(XmlRpcController pController) {
            super(pController);
        }

        @Override
        public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
            switch (pLocalName) {
                case "nil":
                    return new NilParser();
                case I8Serializer.I8_TAG:
                    return new I8Parser();
                default:
                    return super.getParser(pConfig, pContext, pURI, pLocalName);
            }
        }

        @Override
        public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
            if (pObject instanceof Integer) {
                return new IntSerializer();
            }
            if (pObject instanceof Long) {
                return new LongSerializer();
            }
            return super.getSerializer(pConfig, pObject);
        }

    }

    private static class NilParser extends AtomicParser {

        @Override
        public void setResult(String pResult) throws SAXException {
            if (pResult != null && pResult.trim().length() > 0) {
                throw new SAXParseException("Unexpected characters in nil element.", getDocumentLocator());
            }
            super.setResult((Object) null);
        }

    }

    private static class I8Parser extends AtomicParser {

        @Override
        public void setResult(String pResult) throws SAXException {
            super.setResult(Long.getLong(pResult));
        }

    }

    private static class IntSerializer extends TypeSerializerImpl {

        @Override
        public void write(ContentHandler pHandler, Object pObject) throws SAXException {
            write(pHandler, I4Serializer.INT_TAG, pObject.toString());
        }

    }

    private static class LongSerializer extends TypeSerializerImpl {

        @Override
        public void write(ContentHandler pHandler, Object pObject) throws SAXException {
            write(pHandler, I8Serializer.I8_TAG, pObject.toString());
        }

    }

}
