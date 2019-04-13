package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.Constants;
import hudson.plugins.scm.koji.model.RPM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMaps;

public class RPMList implements XmlRpcResponse<List<RPM>> {

    private final List<RPM> rpms;

    public RPMList(List<RPM> rpms) {
        this.rpms = rpms;
    }

    @Override
    public Object toObject() {
        return parseRpms();
    }

    @Override
    public List<RPM> getValue() {
        return rpms;
    }

    private static List<RPM> parseRpmMaps(List<Map<String, Object>> maps) {
        if (maps == null) {
            return Collections.emptyList();
        }
        final List<RPM> rpms = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            rpms.add(parseRpmMap(map));
        }
        return rpms;
    }

    private static RPM parseRpmMap(Map<String, Object> map) {
        return new RPM(
                (String) map.get(Constants.name),
                (String) map.get(Constants.version),
                (String) map.get(Constants.release),
                (String) map.get(Constants.nvr),
                (String) map.get(Constants.arch),
                (String) map.get(Constants.filename)
        );
    }

    private List<Map<String, Object>> parseRpms() {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (RPM rpm : rpms) {
            maps.add(parseRpm(rpm));
        }
        return maps;
    }

    private Map<String, Object> parseRpm(RPM rpm) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.release, rpm.getRelease());
        map.put(Constants.version, rpm.getVersion());
        map.put(Constants.name, rpm.getName());
        map.put(Constants.filename, rpm.getFilename(""));
        map.put(Constants.nvr, rpm.getNvr());
        map.put(Constants.arch, rpm.getArch());
        return map;
    }

    public static RPMList create(Object object) {
        return new RPMList(parseRpmMaps(toMaps(object)));
    }
}
