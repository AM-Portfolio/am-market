package com.am.marketdata.external.api.registry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Registry for managing API endpoints
 */
@Component
@Slf4j
public class ApiEndpointRegistry {
    
    private final Map<String, ApiEndpoint> endpoints = new ConcurrentHashMap<>();
    
    /**
     * Initialize the registry
     */
    @PostConstruct
    public void init() {
        log.info("Initializing API endpoint registry");
    }
    
    /**
     * Register an endpoint
     * 
     * @param endpoint Endpoint to register
     * @return The registered endpoint
     */
    public ApiEndpoint registerEndpoint(ApiEndpoint endpoint) {
        endpoints.put(endpoint.getId(), endpoint);
        
        log.info("Registered API endpoint: {} ({})", endpoint.getName(), endpoint.getId());
        return endpoint;
    }
    
    /**
     * Get an endpoint by ID
     * 
     * @param id Endpoint ID
     * @return The endpoint, or null if not found
     */
    public ApiEndpoint getEndpoint(String id) {
        return endpoints.get(id);
    }
    
    /**
     * Get all registered endpoints
     * 
     * @return Collection of all endpoints
     */
    public Collection<ApiEndpoint> getAllEndpoints() {
        return endpoints.values();
    }
    
    /**
     * Get all endpoints that are enabled for health checks
     * 
     * @return Collection of health check enabled endpoints
     */
    public Collection<ApiEndpoint> getHealthCheckEndpoints() {
        return endpoints.values().stream()
                .filter(ApiEndpoint::isHealthCheckEnabled)
                .toList();
    }
    
    /**
     * Check if an endpoint is registered
     * 
     * @param id Endpoint ID
     * @return true if registered, false otherwise
     */
    public boolean hasEndpoint(String id) {
        return endpoints.containsKey(id);
    }
    
    /**
     * Remove an endpoint
     * 
     * @param id Endpoint ID
     * @return The removed endpoint, or null if not found
     */
    public ApiEndpoint removeEndpoint(String id) {
        ApiEndpoint removed = endpoints.remove(id);
        
        if (removed != null) {
            log.info("Removed API endpoint: {} ({})", removed.getName(), id);
        }
        
        return removed;
    }
    
    /**
     * Clear all endpoints
     */
    public void clearEndpoints() {
        endpoints.clear();
        log.info("Cleared all API endpoints");
    }
    
    /**
     * Get the number of registered endpoints
     * 
     * @return Number of endpoints
     */
    public int getEndpointCount() {
        return endpoints.size();
    }
}
