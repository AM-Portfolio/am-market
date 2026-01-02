package com.am.marketdata.processor.service.common;


import com.am.marketdata.processor.exception.ProcessorException;

/**
 * Interface for processing market data
 * @param <T> The type of data to process
 * @param <R> The result type after processing
 */
public interface DataProcessor<T, R> {
    
    /**
     * Process the data
     * 
     * @param data The data to process
     * @return The processed result
     * @throws ProcessorException if processing fails
     */
    R process(T data) throws ProcessorException;
    
    /**
     * Get the data type name for logging and metrics
     * 
     * @return The data type name
     */
    String getDataTypeName();

}