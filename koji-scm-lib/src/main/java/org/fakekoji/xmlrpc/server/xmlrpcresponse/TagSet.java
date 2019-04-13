package org.fakekoji.xmlrpc.server.xmlrpcresponse;

import hudson.plugins.scm.koji.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fakekoji.xmlrpc.server.xmlrpcrequestparams.XmlRpcRequestUtils.toMaps;

public class TagSet implements XmlRpcResponse<Set<String>>  {

    private final Set<String> tags;

    public TagSet(Set<String> tags) {
        this.tags = tags;
    }

    @Override
    public Object toObject() {
        return parseTags();
    }

    @Override
    public Set<String> getValue() {
        return tags;
    }

    private static Set<String> parseTagMaps(List<Map<String, Object>> maps) {
        final Set<String> tags = new HashSet<>();
        for (Map<String, Object> tagMap : maps) {
            tags.add((String) tagMap.get(Constants.name));
        }
        return tags;
    }

    private List<Map<String, Object>> parseTags() {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (String tag : tags) {
            Map<String, Object> tagMap = new HashMap<>();
            tagMap.put(Constants.name, tag);
            maps.add(tagMap);
        }
        return maps;
    }

    public static TagSet create(Object object) {
        return new TagSet(parseTagMaps(toMaps(object)));
    }
}
