package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMaps;

public class BuildList implements XmlRpcResponse<List<Build>> {

    private final List<Build> builds;

    public BuildList(List<Build> builds) {
        this.builds = builds;
    }

    @Override
    public Object toObject() {
        return parseBuilds();
    }

    @Override
    public List<Build> getValue() {
        return builds;
    }

    private static List<Build> parseBuildMaps(List<Map<String, Object>> maps) {
        if (maps == null) {
            return Collections.emptyList();
        }
        final List<Build> builds = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            builds.add(parseBuildMap(map));
        }
        return builds;
    }

    private static Build parseBuildMap(Map<String, Object> map) {
        return new Build(
                (Integer) map.get(Constants.build_id),
                (String) map.get(Constants.name),
                (String) map.get(Constants.version),
                (String) map.get(Constants.release),
                (String) map.get(Constants.nvr),
                (String) map.get(Constants.completion_time),
                null,
                null,
                null,
                null
        );
    }

    private List<Map<String, Object>> parseBuilds() {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Build build : builds) {
            maps.add(parseBuild(build));
        }
        return maps;
    }

    private Map<String, Object> parseBuild(Build build) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.name, build.getName());
        map.put(Constants.version, build.getVersion());
        map.put(Constants.release, build.getRelease());
        map.put(Constants.nvr, build.getNvr());
        map.put(Constants.build_id, build.getId());
        map.put(Constants.completion_time, build.getCompletionTime());
        // map.put(Constants.rpms, parseRpms(build.getRpms()));
        return map;
    }

    public static BuildList create(Object object) {
        return new BuildList(parseBuildMaps(toMaps(object)));
    }
}
