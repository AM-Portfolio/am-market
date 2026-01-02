package com.am.marketdata.external.api.client;

import java.util.Map;

import com.am.marketdata.external.api.model.ApiResponse;

/**
 * Interface for all external API clients
 */
public interface ApiClient {
    
    /**
     * Sends a GET request to the specified URL
     * 
     * @param url The URL to send the request to
     * @return ApiResponse containing the response data
     */
    ApiResponse get(String url);
    
    /**
     * Sends a GET request to the specified URL with headers
     * 
     * @param url The URL to send the request to
     * @param headers Map of HTTP headers to include in the request
     * @return ApiResponse containing the response data
     */
    ApiResponse get(String url, Map<String, String> headers);
    
    /**
     * Sends a POST request to the specified URL with a request body
     * 
     * @param url The URL to send the request to
     * @param body The request body to send
     * @return ApiResponse containing the response data
     */
    ApiResponse post(String url, Object body);
    
    /**
     * Sends a POST request to the specified URL with a request body and headers
     * 
     * @param url The URL to send the request to
     * @param body The request body to send
     * @param headers Map of HTTP headers to include in the request
     * @return ApiResponse containing the response data
     */
    ApiResponse post(String url, Object body, Map<String, String> headers);
    
    /**
     * Checks if the API is available
     * 
     * @param url The URL to check
     * @return true if available, false otherwise
     */
    boolean isAvailable(String url);
}
