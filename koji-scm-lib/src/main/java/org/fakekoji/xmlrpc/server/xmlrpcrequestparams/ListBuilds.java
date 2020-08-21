package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.starStarLabel;
import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMap;

public class ListBuilds implements XmlRpcRequestParams {

    // always is 1, that means we want successfully completed builds
    private static final String stateLabel = "state";

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListBuilds that = (ListBuilds) o;
        return Objects.equals(packageId, that.packageId) &&
                Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), packageId);
    }
}
