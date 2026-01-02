package com.am.marketdata.external.api.service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.am.marketdata.external.api.client.ApiClient;
import com.am.marketdata.external.api.model.ApiHealthStatus;
import com.am.marketdata.external.api.registry.ApiEndpoint;
import com.am.marketdata.external.api.registry.ApiEndpointRegistry;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for monitoring the health of API endpoints
 */
@Service
@Slf4j
public class ApiHealthService {
    
    private final ApiClient apiClient;
    @Getter
    private final ApiEndpointRegistry endpointRegistry;
    private final Map<String, ApiHealthStatus> healthStatusMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private final long healthCheckIntervalMs;
    private final boolean healthCheckEnabled;
    
    /**
     * Constructor with dependencies
     * 
     * @param apiClient API client for making health check requests
     * @param endpointRegistry Registry of API endpoints
     * @param healthCheckIntervalMs Interval between health checks in milliseconds
     * @param healthCheckEnabled Whether health checks are enabled
     */
    public ApiHealthService(
            ApiClient apiClient,
            ApiEndpointRegistry endpointRegistry,
            @Value("${external.api.health-check.interval-ms:300000}") long healthCheckIntervalMs,
            @Value("${external.api.health-check.enabled:true}") boolean healthCheckEnabled) {
        this.apiClient = apiClient;
        this.endpointRegistry = endpointRegistry;
        this.healthCheckIntervalMs = healthCheckIntervalMs;
        this.healthCheckEnabled = healthCheckEnabled;
    }
    
    /**
     * Initialize the service and start health check scheduler
     */
    @PostConstruct
    public void init() {
        if (healthCheckEnabled) {
            log.info("Starting API health check scheduler with interval {}ms", healthCheckIntervalMs);
            scheduler.scheduleAtFixedRate(this::checkAllEndpoints, 0, healthCheckIntervalMs, TimeUnit.MILLISECONDS);
        } else {
            log.info("API health checks are disabled");
        }
    }
    
    /**
     * Shutdown the scheduler
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down API health check scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Check the health of all endpoints
     */
    public void checkAllEndpoints() {
        Collection<ApiEndpoint> endpoints = endpointRegistry.getHealthCheckEndpoints();
        log.debug("Checking health of {} API endpoints", endpoints.size());
        
        for (ApiEndpoint endpoint : endpoints) {
            try {
                checkEndpoint(endpoint);
            } catch (Exception e) {
                log.error("Error checking health of endpoint {}: {}", endpoint.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Check the health of a specific endpoint
     * 
     * @param endpoint Endpoint to check
     * @return Health status
     */
    public ApiHealthStatus checkEndpoint(ApiEndpoint endpoint) {
        log.debug("Checking health of endpoint: {} ({})", endpoint.getName(), endpoint.getId());
        
        long startTime = System.currentTimeMillis();
        boolean isAvailable = false;
        String errorMessage = null;
        
        try {
            isAvailable = apiClient.isAvailable(endpoint.getUrl());
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.warn("Health check failed for endpoint {}: {}", endpoint.getId(), errorMessage);
        }
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        ApiHealthStatus status;
        if (isAvailable) {
            status = ApiHealthStatus.healthy(
                    endpoint.getId(), 
                    endpoint.getName(), 
                    endpoint.getUrl(), 
                    responseTime);
            log.debug("Endpoint {} is healthy ({}ms)", endpoint.getId(), responseTime);
        } else {
            status = ApiHealthStatus.unhealthy(
                    endpoint.getId(), 
                    endpoint.getName(), 
                    endpoint.getUrl(), 
                    errorMessage != null ? errorMessage : "Endpoint unavailable");
            log.warn("Endpoint {} is unhealthy: {}", endpoint.getId(), errorMessage);
        }
        
        healthStatusMap.put(endpoint.getId(), status);
        return status;
    }
    
    /**
     * Get the health status of all endpoints
     * 
     * @return Collection of health statuses
     */
    public Collection<ApiHealthStatus> getAllHealthStatuses() {
        return healthStatusMap.values();
    }
    
    /**
     * Get the health status of a specific endpoint
     * 
     * @param endpointId Endpoint ID
     * @return Health status, or null if not found
     */
    public ApiHealthStatus getHealthStatus(String endpointId) {
        return healthStatusMap.get(endpointId);
    }
    
    /**
     * Check if all endpoints are healthy
     * 
     * @return true if all endpoints are healthy, false otherwise
     */
    public boolean areAllEndpointsHealthy() {
        return healthStatusMap.values().stream().allMatch(ApiHealthStatus::isAvailable);
    }
}
