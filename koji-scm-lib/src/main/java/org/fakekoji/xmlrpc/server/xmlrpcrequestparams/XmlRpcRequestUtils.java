package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import java.util.Map;

class XmlRpcRequestUtils {

    static final String starStarLabel = "__starstar";
    static final String stateLabel = "state";

    static Map<String, Object> toMap(Object object) {
        return (Map<String, Object>) object;
    }
}
