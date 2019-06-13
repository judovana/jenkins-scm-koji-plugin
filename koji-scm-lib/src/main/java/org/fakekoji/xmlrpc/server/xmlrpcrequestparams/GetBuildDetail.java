package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

public class GetBuildDetail implements XmlRpcRequestParams {

    private final String nvr;

    public GetBuildDetail(String nvr) {
        this.nvr = nvr;
    }

    @Override
    public Object toObject() {
        return this;
    }

    @Override
    public String getMethodName() {
        return Constants.getBuildDetail;
    }

    public String getNvr() {
        return nvr;
    }

    public static GetBuildDetail create(Object object) {
        return (GetBuildDetail) object;
    }
}
