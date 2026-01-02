package com.am.marketdata.external.api.registry;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an API endpoint configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpoint {
    
    /**
     * Unique identifier for the endpoint
     */
    private String id;
    
    /**
     * Human-readable name for the endpoint
     */
    private String name;
    
    /**
     * Base URL for the API
     */
    private String baseUrl;
    
    /**
     * Path to append to the base URL
     */
    private String path;
    
    /**
     * HTTP method (GET, POST, etc.)
     */
    private String method;
    
    /**
     * Default headers to include in requests
     */
    private Map<String, String> defaultHeaders;
    
    /**
     * Whether to include this endpoint in health checks
     */
    private boolean healthCheckEnabled;
    
    /**
     * Gets the full URL for this endpoint
     * 
     * @return The full URL
     */
    public String getUrl() {
        if (path == null || path.isEmpty()) {
            return baseUrl;
        }
        
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl + path.substring(1);
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        } else {
            return baseUrl + path;
        }
    }
    
    /**
     * Gets the default headers for this endpoint
     * 
     * @return Map of headers
     */
    public Map<String, String> getHeaders() {
        return defaultHeaders;
    }
}
