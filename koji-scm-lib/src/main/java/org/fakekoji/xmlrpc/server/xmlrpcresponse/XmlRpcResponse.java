package org.fakekoji.xmlrpc.server.xmlrpcresponse;

public interface XmlRpcResponse <T> {


    /**
     * This method converts POJO to maps of objects or a simple object based on xml-rpc method it is used for. This
     * method is used in fake-koji xml-rpc api to send responses.
     *
     * @see RPMList
     * @see PackageId
     *
     * @return map of objects as Object
     */
    Object toObject();

    /**
     * This method converts a simple object or map of objects to POJO. It is reverse toObject method. This method is
     * used in xml-rpc client to receive responses
     *
     * @see RPMList
     * @see PackageId
     *
     * @return instance of requested type
     */
    T getValue();
}
