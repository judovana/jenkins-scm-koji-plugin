package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class XmlRpcRequestUtils {

    static final String starStarLabel = "__starstar";
    static final String stateLabel = "state";

    static Map<String, Object> toMap(Object object) {
        return (Map<String, Object>) object;
    }

    public static List<Map<String, Object>> toMaps(Object object) {
        final Object[] objectArray = (Object[]) object;
        if (objectArray == null) {
            return null;
        }
        final List<Object> objects = Arrays.asList(objectArray);
        final List<Map<String, Object>> maps = new ArrayList<>(objects.size());
        for (Object o : objects) {
            maps.add(toMap(o));
        }
        return maps;
    }
}
