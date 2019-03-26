package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import java.io.Serializable;

public interface XmlRpcRequestParams extends Serializable {

    Object toObject();
    String getMethodName();

}
