package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import org.fakekoji.xmlrpc.server.expensiveobjectscache.CachableRequest;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;


public interface XmlRpcRequestParams extends Serializable, CachableRequest {

    /**
     * This method converts POJO to a map of objects with additional parameters based on XML-RPC method it is used for.
     * This method is used by XML-RPC client for sending XML-RPC requests.
     *
     * @return map of objects as Object
     * @see ListRPMs
     * @see GetPackageId
     */
    Object[] toXmlRpcParams();

    /**
     * This method returns name of the XML-RPC method that this class represents parameters of.
     *
     * @return XML-RPC method name
     */
    String getMethodName();

    public static String toNiceString(XmlRpcRequestParams o) {
        if (o == null) {
            return "null";
        }
        if (o.toXmlRpcParams() == null || o.toXmlRpcParams().length == 0) {
            return o.getMethodName() + "()";
        }
        return o.getMethodName()+"("+ Arrays.stream(o.toXmlRpcParams()).map(Object::toString).collect(Collectors.joining(","))+")";
    }

}
