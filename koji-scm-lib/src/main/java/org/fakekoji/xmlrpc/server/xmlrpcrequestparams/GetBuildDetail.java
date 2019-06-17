package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import hudson.plugins.scm.koji.Constants;

public class GetBuildDetail implements XmlRpcRequestParams {

    private final String nvr;
    public final String n;
    public final String v;
    public final String r;

    public GetBuildDetail(String n, String v, String r) {
        this.nvr = n + "-" + v + "-" + r;
        this.n=n;
        this.v=v;
        this.r=r;
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
