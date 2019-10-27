package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.model.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FakeBuildList implements XmlRpcResponse<List<Build>> {

    private final List<Build> buildList;

    public FakeBuildList(List<Build> buildList) {
        this.buildList = buildList;
    }

    @Override
    public Object toObject() {
        return buildList;
    }

    @Override
    public List<Build> getValue() {
        return buildList;
    }

    @SuppressWarnings("unchecked")
    public static FakeBuildList create(Object object) {
        final Object[] array = (Object[]) object;
        final List<Build> builds = Stream.of(array)
                .filter(obj -> obj instanceof Build)
                .map(obj -> (Build) obj)
                .collect(Collectors.toList());
        return new FakeBuildList(builds);
    }
}
