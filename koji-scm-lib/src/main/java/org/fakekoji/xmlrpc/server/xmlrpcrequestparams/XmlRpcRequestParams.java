package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import java.io.Serializable;

public interface XmlRpcRequestParams extends Serializable {

    /**
     * This method converts POJO to a map of objects with additional parameters based on XML-RPC method it is used for.
     * This method is used by XML-RPC client for sending XML-RPC requests.
     *
     * @see ListRPMs
     * @see GetPackageId
     *
     * @return map of objects as Object
     */
    Object toObject();

    /**
     *  This method returns name of the XML-RPC method that this class represents parameters of.
     *
     * @return XML-RPC method name
     */
    String getMethodName();

}
