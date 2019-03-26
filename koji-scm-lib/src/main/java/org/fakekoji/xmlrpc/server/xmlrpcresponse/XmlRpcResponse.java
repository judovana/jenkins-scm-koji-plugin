package org.fakekoji.xmlrpc.server.xmlrpcresponse;

public interface XmlRpcResponse <T> {

    Object toObject();
    T getValue();
}
