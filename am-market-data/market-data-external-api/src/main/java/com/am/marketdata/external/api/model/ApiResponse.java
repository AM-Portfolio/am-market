package com.am.marketdata.external.api.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    
    /**
     * HTTP status code
     */
    private int statusCode;
    
    /**
     * Response body
     */
    private String body;
    
    /**
     * Response headers
     */
    private Map<String, String> headers;
    
    /**
     * Response timestamp
     */
    private LocalDateTime timestamp;
    
    /**
     * Flag indicating if the request was successful
     */
    private boolean successful;
    
    /**
     * Error message if the request failed
     */
    private String errorMessage;
    
    /**
     * Time taken for the request in milliseconds
     */
    private long responseTimeMs;
    
    /**
     * Creates a successful response
     * 
     * @param statusCode HTTP status code
     * @param body Response body
     * @param headers Response headers
     * @param responseTimeMs Time taken for the request in milliseconds
     * @return ApiResponse
     */
    public static ApiResponse success(int statusCode, String body, Map<String, String> headers, long responseTimeMs) {
        return ApiResponse.builder()
                .statusCode(statusCode)
                .body(body)
                .headers(headers)
                .timestamp(LocalDateTime.now())
                .successful(true)
                .responseTimeMs(responseTimeMs)
                .build();
    }
    
    /**
     * Creates an error response
     * 
     * @param statusCode HTTP status code
     * @param errorMessage Error message
     * @param responseTimeMs Time taken for the request in milliseconds
     * @return ApiResponse
     */
    public static ApiResponse error(int statusCode, String errorMessage, long responseTimeMs) {
        return ApiResponse.builder()
                .statusCode(statusCode)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .successful(false)
                .responseTimeMs(responseTimeMs)
                .build();
    }
    
    /**
     * Creates a failure response (alias for error)
     * 
     * @param statusCode HTTP status code
     * @param errorMessage Error message
     * @param responseTimeMs Time taken for the request in milliseconds
     * @return ApiResponse
     */
    public static ApiResponse failure(int statusCode, String errorMessage, long responseTimeMs) {
        return error(statusCode, errorMessage, responseTimeMs);
    }
    
    /**
     * Check if the response was successful
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return successful;
    }
    
    /**
     * Get the response data
     * 
     * @return Response body as string
     */
    public String getData() {
        return body;
    }
}
