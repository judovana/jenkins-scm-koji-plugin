package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.HashMap;
import java.util.Map;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.starStarLabel;
import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.stateLabel;
import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMap;

public class ListBuilds implements XmlRpcRequestParams {
    private final Integer packageId;

    public ListBuilds(Integer packageId) {
        this.packageId = packageId;
    }

    @Override
    public Object toObject() {
        final Map<String, Object> map = new HashMap<>();
        map.put(Constants.packageID, packageId);
        map.put(starStarLabel, Boolean.TRUE);
        map.put(stateLabel, 1);
        return map;
    }

    @Override
    public String getMethodName() {
        return Constants.listBuilds;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public static ListBuilds create(Object object) {
        final Map<String, Object> map = toMap(object);
        return new ListBuilds((Integer) map.get(Constants.packageID));
    }
}
