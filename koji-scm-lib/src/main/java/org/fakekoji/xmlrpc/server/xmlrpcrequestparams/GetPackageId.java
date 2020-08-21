package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

import java.util.Objects;

import hudson.plugins.scm.koji.Constants;

public class GetPackageId implements XmlRpcRequestParams {

    private final String packageName;

    public GetPackageId(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public Object toObject() {
        return packageName;
    }

    @Override
    public String getMethodName() {
        return Constants.getPackageID;
    }

    public String getPackageName() {
        return packageName;
    }

    public static GetPackageId create(Object object) {
        return new GetPackageId((String) object);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetPackageId that = (GetPackageId) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMethodName(), packageName);
    }
}
