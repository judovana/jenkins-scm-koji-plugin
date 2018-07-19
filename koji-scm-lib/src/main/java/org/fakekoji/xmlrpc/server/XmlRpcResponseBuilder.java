package org.fakekoji.xmlrpc.server;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.Build;
import hudson.plugins.scm.koji.model.RPM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XmlRpcResponseBuilder {

    private List<Build> builds;
    private List<RPM> rpms;
    private Set<String> tags;
    private Integer packageId;
    private List<String> archives;

    public XmlRpcResponseBuilder() {
        builds = null;
        rpms = null;
        tags = null;
        packageId = null;
        archives = null;
    }

    public void setBuilds(List<Build> builds) {
        this.builds = builds;
    }

    public void setRpms(List<RPM> rpms) {
        this.rpms = rpms;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public void setArchives(List<String> archives) {
        this.archives = archives;
    }

    public XmlRpcResponse build() {
        return new XmlRpcResponse(builds, rpms, tags, packageId, archives);
    }

    public XmlRpcResponse build(String methodName, Object object) {
        if (methodName.equals(Constants.getPackageID) && object instanceof Integer) {
            setPackageId((Integer) object);
            return build();
        }
        if (object instanceof Object[]) {
            final List<Object> list = Arrays.asList((Object[]) object);
            switch (methodName) {
                case Constants.listBuilds:
                    final List<Map<String, Object>> buildMaps = new ArrayList<>(list.size());
                    for (Object o : list) {
                        buildMaps.add((Map<String, Object>) o);
                    }
                    setBuilds(mapsToBuilds(buildMaps));
                    break;
                case Constants.listRPMs:
                    final List<Map<String, Object>> rpmMaps = new ArrayList<>(list.size());
                    for (Object o : list) {
                        rpmMaps.add((Map<String, Object>) o);
                    }
                    setRpms(mapsToRpms(rpmMaps));
                    break;
                case Constants.listArchives:
                    final List<Map<String, Object>> archiveMaps = new ArrayList<>(list.size());
                    for (Object o : list) {
                        archiveMaps.add((Map<String, Object>) o);
                    }
                    setArchives(mapsToArchives(archiveMaps));
                    break;
                case Constants.listTags:
                    final Set<String> tags = new HashSet<>();
                    for (Object o : list) {
                        final Map tag = (Map) o;
                        tags.add((String) tag.get(Constants.name));
                    }
                    setTags(tags);
                    break;
            }
        }
        return build();
    }

    public XmlRpcResponse build(Object object) {
        if (object instanceof Integer) {
            setPackageId((Integer) object);
        }
        return build();
    }

    private List<Build> mapsToBuilds(List<Map<String, Object>> maps) {
        if (maps == null) {
            return Collections.emptyList();
        }
        final List<Build> builds = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            builds.add(mapToBuild(map));
        }
        return builds;
    }

	private Build mapToBuild(Map<String, Object> map) {
		return new Build(
		        (Integer) map.get(Constants.build_id),
                (String) map.get(Constants.name),
                (String) map.get(Constants.version),
                (String) map.get(Constants.release),
                (String) map.get(Constants.nvr),
                (String) map.get(Constants.completion_time),
                null,
                null,
                null
		);
	}

    private List<RPM> mapsToRpms(List<Map<String, Object>> rpmMaps) {
        if (rpmMaps == null) {
            return Collections.emptyList();
        }
        final List<RPM> rpms = new ArrayList<>(rpmMaps.size());
        for (Map<String, Object> map : rpmMaps) {
            rpms.add(mapToRpm(map));
        }
        return rpms;
    }

    private List<String> mapsToArchives(List<Map<String, Object>> archiveMaps) {
        if (archiveMaps == null) {
            return Collections.emptyList();
        }
        final List<String> archives = new ArrayList<>(archiveMaps.size());
        for (Map<String, Object> map : archiveMaps) {
            archives.add(mapToArchive(map));
        }
        return archives;
    }

    private String mapToArchive(Map<String, Object> map) {
        return (String) map.get(Constants.filename);
    }

    private RPM mapToRpm(Map<String, Object> map) {
        return new RPM(
                (String) map.get(Constants.name),
                (String) map.get(Constants.version),
                (String) map.get(Constants.release),
                (String) map.get(Constants.nvr),
                (String) map.get(Constants.arch),
                (String) map.get(Constants.filename)
        );
    }
}
