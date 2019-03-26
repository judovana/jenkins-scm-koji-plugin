package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

import java.util.List;

public class ListRPMs extends ListArchives {

    public ListRPMs(Integer buildId, List<String> archs) {
        super(buildId, archs);
    }

    public ListRPMs(Object object) {
        super(object);
    }

    @Override
    public String getMethodName() {
        return Constants.listRPMs;
    }
}
