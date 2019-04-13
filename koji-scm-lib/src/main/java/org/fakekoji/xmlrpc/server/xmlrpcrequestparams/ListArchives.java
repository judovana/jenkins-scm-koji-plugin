package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.List;
import java.util.Map;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMap;

public class ListArchives extends ListArtefacts {

    public ListArchives(Integer buildId, List<String> archs) {
        super(buildId, archs);
    }

    @Override
    public String getMethodName() {
        return Constants.listArchives;
    }

    public static ListArchives create(Object object) {
        final Map<String, Object> map = toMap(object);
        return new ListArchives(
                (Integer) map.get(Constants.buildID),
                getArchs(map.get(Constants.arches))
        );
    }
}
