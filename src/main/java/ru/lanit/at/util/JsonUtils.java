package ru.lanit.at.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JsonNode getJsonNodeByKey(String json, String key) {
        try {
            return MAPPER.readTree(json).findPath(key);
        } catch (JsonProcessingException e) {
            return MAPPER.missingNode();
        }
    }

}
