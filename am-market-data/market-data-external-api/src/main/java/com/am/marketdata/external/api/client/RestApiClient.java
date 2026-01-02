package com.am.marketdata.external.api.client;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.am.marketdata.external.api.model.ApiResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ApiClient using RestTemplate with resilience patterns
 */
@Component
@Slf4j
public class RestApiClient implements ApiClient {

    private final RestTemplate restTemplate;
    private final int maxRetries;
    private final long baseDelayMs;

    /**
     * Constructor with dependencies
     * 
     * @param restTemplate RestTemplate for making HTTP requests
     * @param maxRetries Maximum number of retry attempts
     * @param baseDelayMs Base delay in milliseconds between retries
     */
    public RestApiClient(
            @Qualifier("externalApiRestTemplate") RestTemplate restTemplate,
            @Value("${external.api.retry.max-attempts:3}") int maxRetries,
            @Value("${external.api.retry.base-delay-ms:1000}") long baseDelayMs) {
        this.restTemplate = restTemplate;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        log.info("RestApiClient initialized with maxRetries={}, baseDelayMs={}", maxRetries, baseDelayMs);
    }

    @Override
    @Retry(name = "externalApiRetry")
    @CircuitBreaker(name = "externalApiCircuitBreaker")
    public ApiResponse get(String url) {
        return get(url, Collections.emptyMap());
    }

    @Override
    @Retry(name = "externalApiRetry")
    @CircuitBreaker(name = "externalApiCircuitBreaker")
    public ApiResponse get(String url, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        log.debug("Making GET request to {}", url);
        
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::add);
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("GET request to {} completed in {}ms with status {}", url, responseTime, response.getStatusCode());
            
            Map<String, String> responseHeaders = new HashMap<>();
            response.getHeaders().forEach((key, value) -> {
                if (!value.isEmpty()) {
                    responseHeaders.put(key, value.get(0));
                }
            });
            
            return ApiResponse.success(
                    response.getStatusCode().value(), 
                    response.getBody(), 
                    responseHeaders, 
                    responseTime);
            
        } catch (HttpStatusCodeException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("HTTP error during GET request to {}: {} ({})", url, e.getMessage(), e.getStatusCode());
            
            return ApiResponse.error(
                    e.getStatusCode().value(), 
                    e.getMessage(), 
                    responseTime);
            
        } catch (ResourceAccessException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Resource access error during GET request to {}: {}", url, e.getMessage());
            
            return ApiResponse.error(
                    HttpStatus.SERVICE_UNAVAILABLE.value(), 
                    "Service unavailable: " + e.getMessage(), 
                    responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during GET request to {}: {}", url, e.getMessage());
            
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                    "Internal error: " + e.getMessage(), 
                    responseTime);
        }
    }

    @Override
    @Retry(name = "externalApiRetry")
    @CircuitBreaker(name = "externalApiCircuitBreaker")
    public ApiResponse post(String url, Object body) {
        return post(url, body, Collections.emptyMap());
    }

    @Override
    @Retry(name = "externalApiRetry")
    @CircuitBreaker(name = "externalApiCircuitBreaker")
    public ApiResponse post(String url, Object body, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        log.debug("Making POST request to {}", url);
        
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            headers.forEach(httpHeaders::add);
            
            HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            log.debug("POST request to {} completed in {}ms with status {}", url, responseTime, response.getStatusCode());
            
            Map<String, String> responseHeaders = new HashMap<>();
            response.getHeaders().forEach((key, value) -> {
                if (!value.isEmpty()) {
                    responseHeaders.put(key, value.get(0));
                }
            });
            
            return ApiResponse.success(
                    response.getStatusCode().value(), 
                    response.getBody(), 
                    responseHeaders, 
                    responseTime);
            
        } catch (HttpStatusCodeException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("HTTP error during POST request to {}: {} ({})", url, e.getMessage(), e.getStatusCode());
            
            return ApiResponse.error(
                    e.getStatusCode().value(), 
                    e.getMessage(), 
                    responseTime);
            
        } catch (ResourceAccessException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Resource access error during POST request to {}: {}", url, e.getMessage());
            
            return ApiResponse.error(
                    HttpStatus.SERVICE_UNAVAILABLE.value(), 
                    "Service unavailable: " + e.getMessage(), 
                    responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during POST request to {}: {}", url, e.getMessage());
            
            return ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                    "Internal error: " + e.getMessage(), 
                    responseTime);
        }
    }

    @Override
    public boolean isAvailable(String url) {
        try {
            ApiResponse response = get(url);
            return response.isSuccessful() && response.getStatusCode() < 400;
        } catch (Exception e) {
            log.warn("Error checking availability of {}: {}", url, e.getMessage());
            return false;
        }
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
}
