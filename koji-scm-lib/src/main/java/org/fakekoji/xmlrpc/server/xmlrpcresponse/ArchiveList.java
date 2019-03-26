package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMaps;

public class ArchiveList implements XmlRpcResponse<List<String>> {

    private final List<String> archives;

    public ArchiveList(List<String> archives) {
        this.archives = archives;
    }

    public ArchiveList(Object object) {
        archives = parseArchiveMaps(toMaps(object));
    }

    @Override
    public Object toObject() {
        return new Object[]{};
    }

    @Override
    public List<String> getValue() {
        return archives;
    }

    private List<String> parseArchiveMaps(List<Map<String, Object>> maps) {
        if (maps == null) {
            return Collections.emptyList();
        }
        final List<String> archives = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            archives.add(parseArchiveMap(map));
        }
        return archives;
    }

    private String parseArchiveMap(Map<String, Object> map) {
        return (String) map.get(Constants.filename);
    }
}
