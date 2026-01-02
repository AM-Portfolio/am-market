package com.am.marketdata.external.api.controller;

import java.util.Collection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.am.marketdata.external.api.model.ApiHealthStatus;
import com.am.marketdata.external.api.service.ApiHealthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for API health check endpoints
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class ApiHealthController {
    
    private final ApiHealthService apiHealthService;
    
    /**
     * Get health status of all API endpoints
     * 
     * @return Collection of health statuses
     */
    @GetMapping
    public ResponseEntity<Collection<ApiHealthStatus>> getAllHealthStatuses() {
        log.debug("Getting health status for all API endpoints");
        return ResponseEntity.ok(apiHealthService.getAllHealthStatuses());
    }
    
    /**
     * Get health status of a specific API endpoint
     * 
     * @param endpointId Endpoint ID
     * @return Health status
     */
    @GetMapping("/{endpointId}")
    public ResponseEntity<ApiHealthStatus> getHealthStatus(@PathVariable String endpointId) {
        log.debug("Getting health status for API endpoint: {}", endpointId);
        ApiHealthStatus status = apiHealthService.getHealthStatus(endpointId);
        
        if (status == null) {
            log.warn("Health status not found for endpoint: {}", endpointId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Check health of a specific API endpoint
     * 
     * @param endpointId Endpoint ID
     * @return Health status
     */
    @GetMapping("/check/{endpointId}")
    public ResponseEntity<ApiHealthStatus> checkEndpoint(@PathVariable String endpointId) {
        log.debug("Checking health of API endpoint: {}", endpointId);
        
        var endpoint = apiHealthService.getEndpointRegistry().getEndpoint(endpointId);
        if (endpoint == null) {
            log.warn("Endpoint not found: {}", endpointId);
            return ResponseEntity.notFound().build();
        }
        
        ApiHealthStatus status = apiHealthService.checkEndpoint(endpoint);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Check health of all API endpoints
     * 
     * @return Collection of health statuses
     */
    @GetMapping("/check/all")
    public ResponseEntity<Collection<ApiHealthStatus>> checkAllEndpoints() {
        log.debug("Checking health of all API endpoints");
        apiHealthService.checkAllEndpoints();
        return ResponseEntity.ok(apiHealthService.getAllHealthStatuses());
    }
    
    /**
     * Get overall health status
     * 
     * @return Status message
     */
    @GetMapping("/status")
    public ResponseEntity<String> getOverallStatus() {
        boolean allHealthy = apiHealthService.areAllEndpointsHealthy();
        log.debug("Getting overall health status: {}", allHealthy ? "healthy" : "unhealthy");
        
        if (allHealthy) {
            return ResponseEntity.ok("All API endpoints are healthy");
        } else {
            return ResponseEntity.status(503).body("One or more API endpoints are unhealthy");
        }
    }
}
