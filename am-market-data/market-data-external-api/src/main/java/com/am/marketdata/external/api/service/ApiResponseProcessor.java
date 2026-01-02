package com.am.marketdata.external.api.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.am.marketdata.external.api.client.ApiClient;
import com.am.marketdata.external.api.model.ApiResponse;
import com.am.marketdata.external.api.registry.ApiEndpoint;
import com.am.marketdata.external.api.registry.ApiEndpointRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for processing API responses with retry and validation
 */
@Service
@Slf4j
public class ApiResponseProcessor {
    
    private final ApiClient apiClient;
    private final ApiEndpointRegistry endpointRegistry;
    private final int maxRetries;
    private final long baseDelayMs;
    
    /**
     * Constructor with dependencies
     * 
     * @param apiClient API client for making requests
     * @param endpointRegistry Registry of API endpoints
     * @param maxRetries Maximum number of retry attempts
     * @param baseDelayMs Base delay in milliseconds between retries
     */
    public ApiResponseProcessor(
            ApiClient apiClient,
            ApiEndpointRegistry endpointRegistry,
            @Value("${external.api.retry.max-attempts:3}") int maxRetries,
            @Value("${external.api.retry.base-delay-ms:1000}") long baseDelayMs) {
        this.apiClient = apiClient;
        this.endpointRegistry = endpointRegistry;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        log.info("ApiResponseProcessor initialized with maxRetries={}, baseDelayMs={}", maxRetries, baseDelayMs);
    }
    
    /**
     * Process an API request with retries and validation
     * 
     * @param <T> Return type
     * @param endpointId ID of the endpoint to call
     * @param processor Function to process the API response
     * @return Processed result, or null if all retries failed
     */
    public <T> T processRequest(String endpointId, Function<ApiResponse, T> processor) {
        ApiEndpoint endpoint = endpointRegistry.getEndpoint(endpointId);
        if (endpoint == null) {
            log.error("Endpoint not found: {}", endpointId);
            return null;
        }
        
        return retryOnFailure(() -> {
            ApiResponse response = apiClient.get(endpoint.getUrl(), endpoint.getHeaders());
            if (!response.isSuccessful()) {
                log.warn("API call to {} failed with status {}: {}", 
                        endpoint.getUrl(), response.getStatusCode(), response.getErrorMessage());
                throw new RuntimeException("API call failed: " + response.getErrorMessage());
            }
            return processor.apply(response);
        });
    }
    
    /**
     * Process an API request asynchronously with retries and validation
     * 
     * @param <T> Return type
     * @param endpointId ID of the endpoint to call
     * @param processor Function to process the API response
     * @return CompletableFuture of the processed result
     */
    public <T> CompletableFuture<T> processRequestAsync(String endpointId, Function<ApiResponse, T> processor) {
        return CompletableFuture.supplyAsync(() -> processRequest(endpointId, processor));
    }
    
    /**
     * Retry a function on failure with exponential backoff
     * 
     * @param <T> Return type
     * @param supplier Function to retry
     * @return Result of the function, or null if all retries failed
     */
    private <T> T retryOnFailure(RetryableSupplier<T> supplier) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxRetries) {
                    long delayMs = calculateRetryDelay(attempt);
                    log.warn("Attempt {} failed, retrying in {}ms: {}", attempt, delayMs, e.getMessage());
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        
        log.error("All {} retry attempts failed", maxRetries, lastException);
        return null;
    }
    
    /**
     * Calculates the delay for a retry attempt using exponential backoff
     * 
     * @param attempt Current attempt number (1-based)
     * @return Delay in milliseconds
     */
    private long calculateRetryDelay(int attempt) {
        // Exponential backoff: baseDelay * 2^(attempt-1)
        return baseDelayMs * (long) Math.pow(2, attempt - 1);
    }
    
    /**
     * Functional interface for a supplier that can throw exceptions
     * 
     * @param <T> Return type
     */
    @FunctionalInterface
    private interface RetryableSupplier<T> {
        T get() throws Exception;
    }
}
