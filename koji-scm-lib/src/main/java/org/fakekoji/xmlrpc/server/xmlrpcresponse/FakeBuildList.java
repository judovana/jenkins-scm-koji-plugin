package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.Nvr;

import java.util.List;

public class FakeBuildList implements XmlRpcResponse<List<Nvr>> {

    private final List<Nvr> buildList;

    public FakeBuildList(List<Nvr> buildList) {
        this.buildList = buildList;
    }

    @Override
    public Object toObject() {
        return this;
    }

    @Override
    public List<Nvr> getValue() {
        return buildList;
    }

    public static FakeBuildList create(Object object) {
        return (FakeBuildList) object;
    }
}
