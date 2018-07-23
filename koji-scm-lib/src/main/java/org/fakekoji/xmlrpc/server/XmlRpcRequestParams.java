package org.fakekoji.xmlrpc.server;

import hudson.plugins.scm.koji.Constants;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlRpcRequestParams implements Serializable {

    private final String packageName;
    private final Integer packageId;
    private final Integer buildId;
    private final Boolean starstar;
    private final Integer state;
    private final List<String> archs;

    public XmlRpcRequestParams(
            String packageName,
            Integer packageId,
            Integer buildId,
            Boolean starstar,
            Integer state,
            List<String> archs
    ) {
        this.packageName = packageName;
        this.packageId = packageId;
        this.buildId = buildId;
        this.starstar = starstar;
        this.state = state;
        this.archs = archs;
    }

    public String getPackageName() {
        return packageName;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public Integer getBuildId() {
        return buildId;
    }

    public Boolean getStarstar() {
        return starstar;
    }

    public Integer getState() {
        return state;
    }

    public List<String> getArchs() {
        return archs;
    }

    public Object toObject(String methodName) {
        if (methodName.equals(Constants.getPackageID)) {
            return packageName;
        }
        return toMap(methodName);
    }

    public Map<String, Object> toMap(String methodName) {
        final Map<String, Object> map = new HashMap<>();
        if (packageId != null) {
            map.put(Constants.packageID, packageId);
        }
        if (buildId != null) {
            switch (methodName) {
                case Constants.listTags:
                    map.put(Constants.build, buildId);
                    break;
                case Constants.listRPMs:
                case Constants.listArchives:
                    map.put(Constants.buildID, buildId);
                    break;
            }
        }
        if (archs != null && !archs.isEmpty()) {
            map.put(Constants.arches, archs.toArray());
        }
        if (starstar != null) {
            map.put("__starstar", starstar);
        }
        if (state != null) {
            map.put("state", state);
        }
        return map;
    }
}
