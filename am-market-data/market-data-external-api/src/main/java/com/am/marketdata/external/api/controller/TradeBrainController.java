package com.am.marketdata.external.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.am.marketdata.external.api.client.TradeBrainClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.external.api.model.EndpointHierarchy;
import com.am.marketdata.external.api.model.EndpointResponse;
import com.am.marketdata.external.api.service.EndpointHealthService;
import com.am.marketdata.external.api.service.EndpointManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for TradeBrain API endpoints
 */
@RestController
@RequestMapping("/api/tradebrain")
@RequiredArgsConstructor
@Slf4j
public class TradeBrainController {
    
    private final TradeBrainClient tradeBrainClient;
    private final EndpointHealthService endpointHealthService;
    private final EndpointManagementService endpointManagementService;
    
    /**
     * Get all market indices
     * 
     * @return List of market indices
     */
    @GetMapping("/indices")
    public ResponseEntity<String> getAllIndices() {
        log.debug("Getting all market indices");
        
        ApiResponse response = tradeBrainClient.getIndicesData();
        
        if (!response.isSuccess()) {
            log.error("Failed to get market indices data: {}", response.getErrorMessage());
            return ResponseEntity.status(response.getStatusCode())
                    .body("Failed to get market indices data: " + response.getErrorMessage());
        }
        
        return ResponseEntity.ok(response.getData());
    }
    
    /**
     * Check health status of all registered endpoints
     * 
     * @return Health status of all endpoints grouped by status with success and failure counts
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkEndpointsHealth() {
        log.debug("Checking health status of all endpoints");
        
        // Use the service to check endpoint health with a 5-second timeout
        Map<String, Object> healthStatus = endpointHealthService.checkEndpointsHealth(5000);
        
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * Get API responses for all registered endpoints
     * 
     * @return List of endpoint responses
     */
    @GetMapping("/api-responses")
    public ResponseEntity<List<EndpointResponse>> getAllApiResponses() {
        log.debug("Getting API responses for all endpoints");
        
        List<EndpointResponse> responses = endpointManagementService.getAllApiResponses();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get all registered endpoints
     * 
     * @return Hierarchical structure of endpoints
     */
    @GetMapping("/endpoints")
    public ResponseEntity<EndpointHierarchy> getAllEndpoints() {
        log.debug("Getting all registered endpoints");
        
        EndpointHierarchy hierarchy = endpointManagementService.getEndpointHierarchy();
        
        return ResponseEntity.ok(hierarchy);
    }
    
    /**
     * Get a specific endpoint by ID
     * 
     * @param endpointId Endpoint ID
     * @return Endpoint response
     */
    @GetMapping("/endpoints/{endpointId}")
    public ResponseEntity<EndpointResponse> getEndpoint(@PathVariable("endpointId") String endpointId) {
        log.debug("Getting endpoint: {}", endpointId);
        
        EndpointResponse response = endpointManagementService.getEndpointResponse(endpointId);
        
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.getStatusCode()).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get stock details for a symbol
     * 
     * @param symbol Stock symbol
     * @return Stock details
     */
    @GetMapping("/stock/{symbol}")
    public ResponseEntity<String> getStockDetails(@PathVariable("symbol") String symbol) {
        log.debug("Getting stock details for symbol: {}", symbol);
        
        ApiResponse response = tradeBrainClient.getStockDetails(symbol);
        
        if (!response.isSuccess()) {
            log.error("Failed to get stock details: {}", response.getErrorMessage());
            return ResponseEntity.status(response.getStatusCode())
                    .body("Failed to get stock details: " + response.getErrorMessage());
        }
        
        return ResponseEntity.ok(response.getData());
    }
}
