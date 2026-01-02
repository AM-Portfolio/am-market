package com.am.marketdata.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * Utility class for generic object mapping operations
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Convert a string or JSON payload to an object of type T
     * 
     * @param <T>      Target type
     * @param payload  JSON string or object to convert
     * @param clazz    Target class type
     * @return         Converted object or null if conversion fails
     */
    public static <T> T convertToType(Object payload, Class<T> clazz) {
        try {
            if (payload instanceof String) {
                return objectMapper.readValue((String) payload, clazz);
            } else if (payload instanceof JsonNode) {
                return objectMapper.treeToValue((JsonNode) payload, clazz);
            } else {
                return objectMapper.convertValue(payload, clazz);
            }
        } catch (JsonProcessingException e) {
            log.error("Error converting payload to type {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Convert a string or JSON payload to a list of objects of type T
     * 
     * @param <T>      Target type
     * @param payload  JSON string or object to convert
     * @param clazz    Target class type
     * @return         List of converted objects or null if conversion fails
     */
    public static <T> List<T> convertToList(Object payload, Class<T> clazz) {
        try {
            if (payload instanceof String) {
                return objectMapper.readValue((String) payload, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
            } else if (payload instanceof JsonNode) {
                return objectMapper.treeToValue((JsonNode) payload, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
            } else {
                return objectMapper.convertValue(payload, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
            }
        } catch (IOException e) {
            log.error("Error converting payload to list of type {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Convert an object to a JSON string
     * 
     * @param object   Object to convert
     * @return         JSON string representation or null if conversion fails
     */
    public static String convertToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert an object to a JsonNode
     * 
     * @param object   Object to convert
     * @return         JsonNode representation or null if conversion fails
     */
    public static JsonNode convertToJsonNode(Object object) {
        try {
            return objectMapper.valueToTree(object);
        } catch (Exception e) {
            log.error("Error converting object to JsonNode: {}", e.getMessage());
            return null;
        }
    }
}