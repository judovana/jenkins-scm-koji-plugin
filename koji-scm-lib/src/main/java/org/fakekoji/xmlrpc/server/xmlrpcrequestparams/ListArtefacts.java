package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.starStarLabel;

public abstract class ListArtefacts implements XmlRpcRequestParams {

    private final Integer buildId;
    private final List<String> archs;

    ListArtefacts(Integer buildId, List<String> archs) {
        this.buildId = buildId;
        this.archs = archs;
    }

    static List<String> getArchs(Object object) {
        if (object == null) {
            return null;
        }
        final List<String> archList = new ArrayList<>();
        final Object[] archs = (Object[]) object;
        for (final Object arch : archs)
            archList.add((String) arch);
        return archList;
    }

    @Override
    public Object[] toXmlRpcParams() {
        final Map<String, Object> map = new HashMap<>();
        map.put(Constants.buildID, buildId);
        map.put(starStarLabel, Boolean.TRUE);
        if (archs != null && !archs.isEmpty()) {
            map.put(Constants.arches, archs.toArray());
        }
        return new Object[]{map};
    }

    public Integer getBuildId() {
        return buildId;
    }

    public List<String> getArchs() {
        return archs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListArtefacts that = (ListArtefacts) o;
        return Objects.equals(buildId, that.buildId) &&
                Objects.equals(archs, that.archs) &&
                Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), buildId, archs);
    }
}
