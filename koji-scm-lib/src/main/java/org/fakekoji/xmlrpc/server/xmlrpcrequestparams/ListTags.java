package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.starStarLabel;
import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMap;

public class ListTags implements XmlRpcRequestParams {

    private final Integer buildId;

    public ListTags(Integer buildId) {
        this.buildId = buildId;
    }

    @Override
    public Object[] toXmlRpcParams() {
        final Map<String, Object> map = new HashMap<>();
        map.put(Constants.build, buildId);
        map.put(starStarLabel, Boolean.TRUE);
        return new Object[]{map};
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListTags listTags = (ListTags) o;
        return Objects.equals(buildId, listTags.buildId) &&
                Objects.equals(getMethodName(), listTags.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), buildId);
    }
}
