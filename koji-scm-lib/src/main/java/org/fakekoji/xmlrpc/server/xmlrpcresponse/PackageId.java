package org.fakekoji.xmlrpc.server.xmlrpcresponse;

public class PackageId implements XmlRpcResponse<Integer> {

    private final Integer packageId;

    public PackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public PackageId(Object object) {
        packageId = (Integer) object;
    }

    @Override
    public Object toObject() {
        return packageId;
    }

    @Override
    public Integer getValue() {
        return packageId;
    }
}
