package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.List;
import java.util.Map;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMap;

public class ListRPMs extends ListArtefacts {

    public ListRPMs(Integer buildId, List<String> archs) {
        super(buildId, archs);
    }

    @Override
    public String getMethodName() {
        return Constants.listRPMs;
    }

    public static ListRPMs create(Object object) {
        final Map<String, Object> map = toMap(object);
        return new ListRPMs(
                (Integer) map.get(Constants.buildID),
                getArchs(map.get(Constants.arches))
        );
    }
}
