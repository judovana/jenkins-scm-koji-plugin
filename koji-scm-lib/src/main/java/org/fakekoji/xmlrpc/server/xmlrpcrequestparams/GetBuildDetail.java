package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBuildDetail that = (GetBuildDetail) o;
        return Objects.equals(n, that.n) &&
                Objects.equals(v, that.v) &&
                Objects.equals(r, that.r) &&
                Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), n, v, r);
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
