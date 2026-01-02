package com.am.common.investment.app.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for JSON operations, particularly focused on model deserialization
 * with proper error handling and logging.
 */
public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule());

    /**
     * Deserialize JSON string into a specified model class
     *
     * @param json JSON string to deserialize
     * @param clazz Target model class
     * @param <T> Type parameter for the target model
     * @return Deserialized model instance or null if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Read JSON from classpath resource and deserialize into a specified model class
     *
     * @param resourcePath Path to the resource file in classpath
     * @param clazz Target model class
     * @param <T> Type parameter for the target model
     * @return Deserialized model instance or null if reading/deserialization fails
     */
    public static <T> T fromResource(String resourcePath, Class<T> clazz) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream is = resource.getInputStream()) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return fromJson(json, clazz);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Serialize model to JSON string
     *
     * @param model Model instance to serialize
     * @return JSON string or null if serialization fails
     */
    public static String toJson(Object model) {
        try {
            return objectMapper.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    /**
     * Get the configured ObjectMapper instance
     * 
     * @return ObjectMapper instance with proper configuration
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private JsonUtils() {
        // Prevent instantiation
    }
}
