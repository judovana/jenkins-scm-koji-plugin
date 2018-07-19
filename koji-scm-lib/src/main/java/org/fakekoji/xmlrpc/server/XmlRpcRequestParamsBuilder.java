package org.fakekoji.xmlrpc.server;

import hudson.plugins.scm.koji.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XmlRpcRequestParamsBuilder {

    private String packageName;
    private Integer packageId;
    private Integer buildId;
    private Boolean starstar;
    private Integer state;
    private List<String> archs;

    public XmlRpcRequestParamsBuilder() {
        this.packageName = null;
        this.packageId = null;
        this.buildId = null;
        this.starstar = null;
        this.state = null;
        this.archs = null;
    }

    public XmlRpcRequestParamsBuilder setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public XmlRpcRequestParamsBuilder setPackageId(Integer packageId) {
        this.packageId = packageId;
        return this;
    }

    public XmlRpcRequestParamsBuilder setBuildId(Integer buildId) {
        this.buildId = buildId;
        return this;
    }

    public XmlRpcRequestParamsBuilder setStarstar(Boolean starstar) {
        this.starstar = starstar;
        return this;
    }

    public XmlRpcRequestParamsBuilder setState(Integer state) {
        this.state = state;
        return this;
    }

    public XmlRpcRequestParamsBuilder setArchs(List<String> archs) {
        this.archs = archs;
        return this;
    }

    public XmlRpcRequestParams build() {
        return new XmlRpcRequestParams(
                packageName,
                packageId,
                buildId,
                starstar,
                state,
                archs
        );
    }

    public XmlRpcRequestParams build(Object object, String methodName) {
        if (object instanceof String) {
            return setPackageName((String) object).build();
        }
        return build((Map<String, Object>) object, methodName);
    }

    public XmlRpcRequestParams build(Map<String, Object> map, String methodName) {
        final Object[] archs = (Object[]) map.get(Constants.arches);
        final List<String> archList = new ArrayList<>();
        if (archs != null) {
            for (final Object arch : archs) {
                archList.add((String) arch);
            }
        }
        return new XmlRpcRequestParams(
                null,
                (Integer) map.get(Constants.packageID),
                retrieveBuildId(map, methodName),
                (Boolean) map.get("__starstar"),
                (Integer) map.get("state"),
                archList
        );
    }

    private Integer retrieveBuildId(Map<String, Object> map, String methodName) {
        // listTags method requires 'build' as key and listRPMs and listArchives methods require buildID as key (sigh)
        switch (methodName) {
            case Constants.listTags:
                return (Integer) map.get(Constants.build);
            case Constants.listRPMs:
            case Constants.listArchives:
                return (Integer) map.get(Constants.buildID);
            default:
                return null;
        }
    }
}
