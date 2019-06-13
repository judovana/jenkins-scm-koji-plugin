package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.model.Build;

public class FakeBuildDetail implements XmlRpcResponse<Build> {

    private final Build build;

    public FakeBuildDetail(Build build) {
        this.build = build;
    }

    @Override
    public Object toObject() {
        return this;
    }

    @Override
    public Build getValue() {
        return build;
    }
}
