package com.am.marketdata.processor.exception;

import lombok.Getter;

/**
 * Specialized exception for data processing errors
 * Based on the comprehensive error handling pattern
 */
@Getter
public class ProcessorException extends RuntimeException {
    
    private final String dataSource;
    private final ProcessorErrorType errorType;


    public ProcessorException(String dataSource, ProcessorErrorType errorType, String message) {
        super(message);
        this.dataSource = dataSource;
        this.errorType = errorType;
    }
    
    public ProcessorException(String dataSource, ProcessorErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.dataSource = dataSource;
        this.errorType = errorType;
    }
    
    /**
     * Categorized error types for better handling and metrics
     */
    public enum ProcessorErrorType {
        INVALID_DATA,
        EXTRACTION_ERROR,
        VALIDATION_ERROR, 
        TRANSFORMATION_ERROR,
        PERSISTENCE_ERROR,
        KAFKA_ERROR,
        UNEXPECTED_ERROR,
        RETRY_INTERRUPTED
    }
}
