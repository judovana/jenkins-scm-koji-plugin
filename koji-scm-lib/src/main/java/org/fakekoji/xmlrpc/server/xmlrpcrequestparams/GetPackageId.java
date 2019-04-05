package org.fakekoji.xmlrpc.server.xmlrpcrequestparams;

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
}
