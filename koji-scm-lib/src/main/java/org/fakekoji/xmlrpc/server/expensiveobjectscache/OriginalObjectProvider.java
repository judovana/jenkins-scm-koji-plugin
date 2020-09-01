package org.fakekoji.xmlrpc.server.expensiveobjectscache;

import org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestParams;

public interface OriginalObjectProvider {

    Object obtainOriginal(String url, XmlRpcRequestParams params);
}
