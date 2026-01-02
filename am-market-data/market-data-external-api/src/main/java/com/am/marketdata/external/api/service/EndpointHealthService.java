package com.am.marketdata.external.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.am.marketdata.external.api.client.ApiClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.external.api.registry.ApiEndpoint;
import com.am.marketdata.external.api.registry.ApiEndpointRegistry;

@Service
public class EndpointHealthService {
    private static final Logger log = LoggerFactory.getLogger(EndpointHealthService.class);
    
    private final ApiClient apiClient;
    private final ApiEndpointRegistry endpointRegistry;
    private final long healthCheckTimeoutMs;
    private final String defaultSymbol;
    
    public EndpointHealthService(
        ApiClient apiClient,
        ApiEndpointRegistry endpointRegistry,
        @Value("${external.api.health-check.timeout-ms:4000}") long healthCheckTimeoutMs,
        @Value("${external.api.health-check.default-symbol:RELIANCE}") String defaultSymbol
    ) {
        this.apiClient = apiClient;
        this.endpointRegistry = endpointRegistry;
        this.healthCheckTimeoutMs = healthCheckTimeoutMs;
        this.defaultSymbol = defaultSymbol;
    }

    /**
     * Check health status of all registered endpoints with a delay between checks
     * 
     * @param timeoutMs Timeout in milliseconds between endpoint checks
     * @return Health status of all endpoints
     */
    public Map<String, Object> checkEndpointsHealth(long timeoutMs) {
        log.debug("Checking health status of all endpoints with timeout: {}ms", timeoutMs);
        
        // If no timeout is provided, use the configured default
        if (timeoutMs <= 0) {
            timeoutMs = healthCheckTimeoutMs;
            log.debug("Using default timeout: {}ms", timeoutMs);
        }
        
        Map<String, Object> result = new HashMap<>();
        Map<String, List<EndpointHealth>> groupedEndpoints = new HashMap<>();
        
        // Initialize status groups
        groupedEndpoints.put("healthy", new ArrayList<>());
        groupedEndpoints.put("unhealthy", new ArrayList<>());
        
        int successCount = 0;
        int failureCount = 0;
        
        // Check each endpoint
        for (ApiEndpoint endpoint : endpointRegistry.getAllEndpoints()) {
            String endpointId = endpoint.getId();
            boolean requiresSymbol = endpoint.getUrl().contains("{symbol}");
            String symbol = requiresSymbol ? defaultSymbol : null;
            
            ApiResponse response;
            long durationMs;
            if (requiresSymbol) {
                response = callEndpointWithSymbolAndTimeout(endpointId, symbol, timeoutMs);
                durationMs = getDurationMs(response);
            } else {
                response = callEndpointWithTimeout(endpointId, timeoutMs);
                durationMs = getDurationMs(response);
            }
            
            EndpointHealth health = createEndpointHealth(endpointId, response, durationMs);
            
            // Group by status
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                groupedEndpoints.get("healthy").add(health);
                successCount++;
            } else {
                groupedEndpoints.get("unhealthy").add(health);
                failureCount++;
            }
        }
        
        // Add status groups to result
        result.put("healthy", groupedEndpoints.get("healthy"));
        result.put("unhealthy", groupedEndpoints.get("unhealthy"));
        
        // Add summary statistics
        result.put("totalEndpoints", successCount + failureCount);
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("successRate", successCount > 0 ? 
            String.format("%.2f%%", (double) successCount / (successCount + failureCount) * 100) : "0.00%" );
        
        return result;
    }

    private long getDurationMs(ApiResponse response) {
        return response.getResponseTimeMs();
    }

    private EndpointHealth createEndpointHealth(String endpointId, ApiResponse response, long durationMs) {
        ApiEndpoint endpoint = endpointRegistry.getEndpoint(endpointId);
        if (endpoint == null) {
            log.error("Endpoint not found: {}", endpointId);
            return new EndpointHealth(endpointId, response.getStatusCode(), response.getErrorMessage(), durationMs, "");
        }
        return new EndpointHealth(endpointId, response.getStatusCode(), response.getErrorMessage(), durationMs, endpoint.getPath());
    }

    private ApiResponse callEndpointWithTimeout(String endpointId, long timeoutMs) {
        ApiEndpoint endpoint = endpointRegistry.getEndpoint(endpointId);
        if (endpoint == null) {
            log.error("Endpoint not found: {}", endpointId);
            return ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Endpoint not found: " + endpointId,
                    0
            );
        }

        CompletableFuture<ApiResponse> future = CompletableFuture.supplyAsync(() -> 
            apiClient.get(endpoint.getUrl())
        );
        
        try {
            long startTime = System.currentTimeMillis();
            ApiResponse response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            long durationMs = System.currentTimeMillis() - startTime;
            response.setResponseTimeMs(durationMs);
            return response;
        } catch (TimeoutException e) {
            log.warn("Timeout after {}ms while checking endpoint health: {}", 
                    timeoutMs, endpointId);
            return ApiResponse.error(
                    HttpStatus.REQUEST_TIMEOUT.value(),
                    "Request timed out after " + timeoutMs + "ms",
                    timeoutMs
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while checking endpoint health: {}", endpointId);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Health check was interrupted",
                    0
            );
        } catch (ExecutionException e) {
            log.error("Error while checking endpoint health: {}", endpointId, e);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error during health check: " + e.getMessage(),
                    0
            );
        }
    }

    private ApiResponse callEndpointWithSymbolAndTimeout(String endpointId, String symbol, long timeoutMs) {
        ApiEndpoint endpoint = endpointRegistry.getEndpoint(endpointId);
        if (endpoint == null) {
            log.error("Endpoint not found: {}", endpointId);
            return ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "Endpoint not found: " + endpointId,
                    0
            );
        }

        String url = endpoint.getUrl().replace("{symbol}", symbol);
        
        CompletableFuture<ApiResponse> future = CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.get(url);
            } catch (Exception e) {
                log.error("Error calling endpoint with symbol: {}, {}", endpointId, symbol, e);
                return ApiResponse.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Error calling endpoint with symbol: " + e.getMessage(),
                        0
                );
            }
        });
        
        try {
            long startTime = System.currentTimeMillis();
            ApiResponse response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            long durationMs = System.currentTimeMillis() - startTime;
            response.setResponseTimeMs(durationMs);
            return response;
        } catch (TimeoutException e) {
            log.warn("Timeout after {}ms while checking endpoint health with symbol {}: {}", 
                    timeoutMs, symbol, endpointId);
            return ApiResponse.error(
                    HttpStatus.REQUEST_TIMEOUT.value(),
                    "Request timed out after " + timeoutMs + "ms",
                    timeoutMs
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while checking endpoint health with symbol {}: {}", symbol, endpointId);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Health check was interrupted",
                    0
            );
        } catch (ExecutionException e) {
            log.error("Error while checking endpoint health with symbol {}: {}", symbol, endpointId, e);
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error during health check: " + e.getMessage(),
                    0
            );
        }
    }

    private record EndpointHealth(
        String endpointId,
        int statusCode,
        String errorMessage,
        long durationMs,
        String path
    ) {}
}
