// DO NOT EDIT: THIS FILE IS AUTO-GENERATED
package com.am.logging;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generated Pattern: [{timestamp}] | [{service}] | [{trace_id}:{span_id}] | [{level}] | [{class}.{method}] | {message} | {context}
 */
public class AMLogger {
    private String serviceName;
    private String clsUrl;
    private boolean persistToDb;
    private static final ObjectMapper mapper = new ObjectMapper();

    public AMLogger(String serviceName, String clsUrl) {
        this(serviceName, clsUrl, Boolean.parseBoolean(System.getenv().getOrDefault("AM_LOGGING_PERSIST_TO_DB", "false")));
    }

    public AMLogger(String serviceName, String clsUrl, boolean persistToDb) {
        this.serviceName = serviceName;
        this.clsUrl = clsUrl;
        this.persistToDb = persistToDb;
    }

    public void log(String level, String message, Map<String, Object> context) {
        log(level, message, context, this.persistToDb);
    }

    public void log(String level, String message, Map<String, Object> context, boolean persistToDb) {
        String timestamp = Instant.now().toString();
        String traceId = UUID.randomUUID().toString();
        String spanId = "root";
        
        // Enforcing Pattern: [{timestamp}] | [{service}] | [{trace_id}:{span_id}] | [{level}] | [{class}.{method}] | {message} | {context}
        String formatted = String.format("[%s] | [%s] | [%s:%s] | [%s] | [%s.%s] | %s | %s",
            timestamp, serviceName, traceId, spanId, level, "Global", "method", message, serialize(context));
            
        System.out.println(formatted);
        // CLS push implementation would go here (using Spring RestTemplate or similar)
    }

    private String serialize(Map<String, Object> context) {
        try { return mapper.writeValueAsString(context); } 
        catch (Exception e) { return "{}"; }
    }
}