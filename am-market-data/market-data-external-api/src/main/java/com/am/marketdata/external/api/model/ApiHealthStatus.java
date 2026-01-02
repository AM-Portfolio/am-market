package com.am.marketdata.external.api.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the health status of an API endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiHealthStatus {
    
    /**
     * Endpoint ID
     */
    private String endpointId;
    
    /**
     * Endpoint name
     */
    private String endpointName;
    
    /**
     * URL of the endpoint
     */
    private String url;
    
    /**
     * Whether the endpoint is available
     */
    private boolean available;
    
    /**
     * Last check timestamp
     */
    private LocalDateTime lastChecked;
    
    /**
     * Response time in milliseconds
     */
    private long responseTimeMs;
    
    /**
     * Error message if the endpoint is unavailable
     */
    private String errorMessage;
    
    /**
     * Creates a healthy status
     * 
     * @param endpointId Endpoint ID
     * @param endpointName Endpoint name
     * @param url URL of the endpoint
     * @param responseTimeMs Response time in milliseconds
     * @return ApiHealthStatus
     */
    public static ApiHealthStatus healthy(String endpointId, String endpointName, String url, long responseTimeMs) {
        return ApiHealthStatus.builder()
                .endpointId(endpointId)
                .endpointName(endpointName)
                .url(url)
                .available(true)
                .lastChecked(LocalDateTime.now())
                .responseTimeMs(responseTimeMs)
                .build();
    }
    
    /**
     * Creates an unhealthy status
     * 
     * @param endpointId Endpoint ID
     * @param endpointName Endpoint name
     * @param url URL of the endpoint
     * @param errorMessage Error message
     * @return ApiHealthStatus
     */
    public static ApiHealthStatus unhealthy(String endpointId, String endpointName, String url, String errorMessage) {
        return ApiHealthStatus.builder()
                .endpointId(endpointId)
                .endpointName(endpointName)
                .url(url)
                .available(false)
                .lastChecked(LocalDateTime.now())
                .errorMessage(errorMessage)
                .build();
    }
}
