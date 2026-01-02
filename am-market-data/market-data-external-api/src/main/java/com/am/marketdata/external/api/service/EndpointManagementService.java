package com.am.marketdata.external.api.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.am.marketdata.external.api.client.TradeBrainClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.external.api.model.EndpointHierarchy;
import com.am.marketdata.external.api.model.EndpointResponse;
import com.am.marketdata.external.api.registry.ApiEndpoint;
import com.am.marketdata.external.api.registry.ApiEndpointRegistry;

/**
 * Service for managing and interacting with API endpoints
 */
@Service
public class EndpointManagementService {
    private static final Logger log = LoggerFactory.getLogger(EndpointManagementService.class);
    
    private final TradeBrainClient tradeBrainClient;
    private final ApiEndpointRegistry endpointRegistry;
    private final String defaultSymbol;
    
    public EndpointManagementService(
        TradeBrainClient tradeBrainClient,
        ApiEndpointRegistry endpointRegistry,
        @Value("${external.api.health-check.default-symbol:RELIANCE}") String defaultSymbol
    ) {
        this.tradeBrainClient = tradeBrainClient;
        this.endpointRegistry = endpointRegistry;
        this.defaultSymbol = defaultSymbol;
    }
    
    /**
     * Get API responses for all registered endpoints
     * 
     * @return List of endpoint responses
     */
    public List<EndpointResponse> getAllApiResponses() {
        log.debug("Getting API responses for all endpoints");
        
        List<EndpointResponse> responses = new ArrayList<>();
        Collection<ApiEndpoint> endpoints = endpointRegistry.getAllEndpoints();
        
        for (ApiEndpoint endpoint : endpoints) {
            String endpointId = endpoint.getId();
            
            try {
                ApiResponse response;
                if (endpoint.getUrl().contains("{symbol}")) {
                    // For endpoints that require a symbol, use the default symbol
                    response = tradeBrainClient.checkEndpointHealthWithSymbol(endpointId, defaultSymbol);
                } else {
                    response = tradeBrainClient.checkEndpointHealth(endpointId);
                }
                
                // Create endpoint response
                EndpointResponse endpointResponse = EndpointResponse.builder()
                    .endpointId(endpointId)
                    .url(endpoint.getUrl())
                    .statusCode(response.getStatusCode())
                    .responseTime(response.getResponseTimeMs())
                    .data(response.getData())
                    .errorMessage(response.getErrorMessage())
                    .success(response.isSuccess())
                    .build();
                
                responses.add(endpointResponse);
            } catch (Exception e) {
                log.error("Error calling endpoint: {}", endpointId, e);
                
                EndpointResponse errorResponse = EndpointResponse.builder()
                    .endpointId(endpointId)
                    .url(endpoint.getUrl())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .errorMessage("Failed to call endpoint: " + e.getMessage())
                    .success(false)
                    .build();
                
                responses.add(errorResponse);
            }
        }
        
        return responses;
    }
    
    /**
     * Get all registered endpoints in a hierarchical structure
     * 
     * @return Hierarchical structure of endpoints
     */
    public EndpointHierarchy getEndpointHierarchy() {
        log.debug("Getting all registered endpoints");
        
        Collection<ApiEndpoint> allEndpoints = endpointRegistry.getAllEndpoints();
        Map<String, Object> groupedEndpoints = new HashMap<>();
        
        for (ApiEndpoint endpoint : allEndpoints) {
            String id = endpoint.getId();
            
            // Skip non-TradeBrain endpoints
            if (!id.startsWith("tradebrain.")) {
                continue;
            }
            
            // Create endpoint info map
            Map<String, Object> endpointInfo = new HashMap<>();
            endpointInfo.put("id", id);
            endpointInfo.put("name", endpoint.getName());
            endpointInfo.put("url", endpoint.getUrl());
            endpointInfo.put("method", endpoint.getMethod());
            
            // Split the ID into parts and add to the hierarchy
            String[] parts = id.split("\\.");
            addToHierarchy(groupedEndpoints, parts, 0, endpointInfo);
        }
        
        return EndpointHierarchy.builder()
            .endpoints(groupedEndpoints)
            .totalEndpoints(allEndpoints.size())
            .build();
    }
    
    /**
     * Recursively add an endpoint to the hierarchical structure
     * 
     * @param current Current level in the hierarchy
     * @param parts Parts of the endpoint ID
     * @param index Current index in the parts array
     * @param endpointInfo Endpoint information
     */
    @SuppressWarnings("unchecked")
    private void addToHierarchy(Map<String, Object> current, String[] parts, int index, Map<String, Object> endpointInfo) {
        if (index >= parts.length) {
            return;
        }
        
        String part = parts[index];
        
        if (index == parts.length - 1) {
            // Last part, add the endpoint info
            current.put(part, endpointInfo);
        } else {
            // Not the last part, create or get the next level
            if (!current.containsKey(part)) {
                current.put(part, new HashMap<String, Object>());
            }
            
            // Recursively add to the next level
            addToHierarchy((Map<String, Object>) current.get(part), parts, index + 1, endpointInfo);
        }
    }
    
    /**
     * Get a specific endpoint by ID
     * 
     * @param endpointId Endpoint ID
     * @return Endpoint response
     */
    public EndpointResponse getEndpointResponse(String endpointId) {
        log.debug("Getting endpoint response for: {}", endpointId);
        
        ApiEndpoint endpoint = endpointRegistry.getEndpoint(endpointId);
        if (endpoint == null) {
            log.error("Endpoint not found: {}", endpointId);
            return EndpointResponse.builder()
                .endpointId(endpointId)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .errorMessage("Endpoint not found: " + endpointId)
                .success(false)
                .build();
        }
        
        try {
            ApiResponse response;
            if (endpoint.getUrl().contains("{symbol}")) {
                // For endpoints that require a symbol, use the default symbol
                response = tradeBrainClient.checkEndpointHealthWithSymbol(endpointId, defaultSymbol);
            } else {
                response = tradeBrainClient.checkEndpointHealth(endpointId);
            }
            
            return EndpointResponse.builder()
                .endpointId(endpointId)
                .url(endpoint.getUrl())
                .statusCode(response.getStatusCode())
                .responseTime(response.getResponseTimeMs())
                .data(response.getData())
                .errorMessage(response.getErrorMessage())
                .success(response.isSuccess())
                .build();
        } catch (Exception e) {
            log.error("Error calling endpoint: {}", endpointId, e);
            
            return EndpointResponse.builder()
                .endpointId(endpointId)
                .url(endpoint.getUrl())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorMessage("Failed to call endpoint: " + e.getMessage())
                .success(false)
                .build();
        }
    }
}
