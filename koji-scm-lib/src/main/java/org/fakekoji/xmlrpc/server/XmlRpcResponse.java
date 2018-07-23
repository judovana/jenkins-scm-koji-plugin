package org.fakekoji.xmlrpc.server;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XmlRpcResponse implements Serializable {

    private final List<Build> builds;
    private final List<RPM> rpms;
    private final Set<String> tags;
    private final Integer packageId;
    private final List<String> archives;

    public XmlRpcResponse(
            List<Build> builds,
            List<RPM> rpms,
            Set<String> tags,
            Integer packageId,
            List<String> archives
    ) {
        this.builds = builds;
        this.rpms = rpms;
        this.tags = tags;
        this.packageId = packageId;
        this.archives = archives;
    }

    public List<Build> getBuilds() {
        return builds;
    }

    public List<RPM> getRpms() {
        return rpms;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public List<String> getArchives() {
        return archives;
    }

    public Object toObject() {
        if (builds != null) {
            return buildsToListOfMaps(builds);
        }
        if (rpms != null) {
            return rpmsToListOfMaps(rpms);
        }
        if (tags != null) {
            return tagsToListOfMaps(tags);
        }
        if (packageId != null) {
            return packageId;
        }
        return null;
    }

    private List<Map<String, Object>> rpmsToListOfMaps(List<RPM> rpms) {
        List<Map<String, Object>> rpmMapsList = new ArrayList<>();
        for (RPM rpm : rpms) {
            rpmMapsList.add(rpmToMap(rpm));
        }
        return rpmMapsList;
    }

    private Map<String, Object> rpmToMap(RPM rpm) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.release, rpm.getRelease());
        map.put(Constants.version, rpm.getVersion());
        map.put(Constants.name, rpm.getName());
        map.put(Constants.filename, rpm.getFilename(""));
        map.put(Constants.nvr, rpm.getNvr());
        map.put(Constants.arch, rpm.getArch());
        return map;
    }

    private List<Map<String, Object>> buildsToListOfMaps(List<Build> builds) {
        List<Map<String, Object>> buildMapsList = new ArrayList<>();
        for (Build build : builds) {
            buildMapsList.add(buildToMap(build));
        }
        return buildMapsList;
    }

    private List<Map<String, Object>> tagsToListOfMaps(Set<String> tags) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (String tag : tags) {
            Map<String, Object> tagMap = new HashMap<>();
            tagMap.put(Constants.name, tag);
            mapList.add(tagMap);
        }
        return mapList;
    }

    private Map<String, Object> buildToMap(Build build) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.name, build.getName());
        map.put(Constants.version, build.getVersion());
        map.put(Constants.release, build.getRelease());
        map.put(Constants.nvr, build.getNvr());
        map.put(Constants.build_id, build.getId());
        map.put(Constants.completion_time, build.getCompletionTime());
        map.put(Constants.rpms, rpmsToListOfMaps(build.getRpms()));
        return map;
    }
}
