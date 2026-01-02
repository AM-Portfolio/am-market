package com.am.marketdata.processor.service.common;

/**
 * Interface for validating market data
 * @param <T> The type of data to validate
 */
public interface DataValidator<T> {
    
    /**
     * Validates the data
     * 
     * @param data The data to validate
     * @return true if the data is valid, false otherwise
     */
    boolean isValid(T data);
    
    /**
     * Get the data type name for logging and metrics
     * 
     * @return The data type name
     */
    String getDataTypeName();
}