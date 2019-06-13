package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.model.Build;

import java.util.List;

public class FakeBuildList implements XmlRpcResponse<List<Build>> {

    private final List<Build> buildList;

    public FakeBuildList(List<Build> buildList) {
        this.buildList = buildList;
    }

    @Override
    public Object toObject() {
        return this;
    }

    @Override
    public List<Build> getValue() {
        return buildList;
    }
}
