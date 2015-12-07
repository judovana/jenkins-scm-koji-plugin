package hudson.plugins.scm.koji.client;

import java.net.URL;
import java.util.Arrays;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.AtomicParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.I4Serializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import hudson.plugins.scm.koji.WebLog;

abstract public class AbstractKojiClient extends AbstractLoggingWorker {

    private final String kojiTopUrl;

    protected AbstractKojiClient(WebLog log, String kojiTopUrl) {
        super(log);
        this.kojiTopUrl = kojiTopUrl;
    }

    protected Object execute(String methodName, Object... args) {
        try {
            XmlRpcClient client = createClient();
            Object res = client.execute(methodName, Arrays.asList(args));
            return res;
        } catch (Exception ex) {
            throw new RuntimeException("Exception while executing " + methodName, ex);
        }
    }

    private XmlRpcClient createClient() throws Exception {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(kojiTopUrl));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        client.setTypeFactory(new KojiTypeFactory(client));
        return client;
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
                default:
                    return super.getParser(pConfig, pContext, pURI, pLocalName);
            }
        }

        @Override
        public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
            if (pObject instanceof Integer) {
                return new IntSerializer();
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

    private static class IntSerializer extends TypeSerializerImpl {

        @Override
        public void write(ContentHandler pHandler, Object pObject) throws SAXException {
            write(pHandler, I4Serializer.INT_TAG, pObject.toString());
        }

    }
}
