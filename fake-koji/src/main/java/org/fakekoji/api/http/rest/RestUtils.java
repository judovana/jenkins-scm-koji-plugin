package org.fakekoji.api.http.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class RestUtils {

    static Optional<String> extractParamValue(Map<String, List<String>> paramsMap, String param) {
        return Optional.ofNullable(paramsMap.get(param))
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0));
    }

}
