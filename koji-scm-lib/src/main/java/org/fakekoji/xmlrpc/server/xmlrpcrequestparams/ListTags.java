package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.HashMap;
import java.util.Map;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.starStarLabel;
import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMap;

public class ListTags implements XmlRpcRequestParams {

    private final Integer buildId;

    public ListTags(Integer buildId) {
        this.buildId = buildId;
    }

    @Override
    public Object toObject() {
        final Map<String, Object> map = new HashMap<>();
        map.put(Constants.build, buildId);
        map.put(starStarLabel, Boolean.TRUE);
        return map;
    }

    @Override
    public String getMethodName() {
        return Constants.listTags;
    }

    public Integer getBuildId() {
        return buildId;
    }

    public static ListTags create(Object object) {
        final Map<String, Object> map = toMap(object);
        return new ListTags((Integer) map.get(Constants.build));
    }
}
