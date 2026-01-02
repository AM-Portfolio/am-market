package com.am.marketdata.processor.service.common;

import com.am.marketdata.processor.exception.DataValidationException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Template for market data operations implementing the Template Method Pattern
 * @param <T> The type of data to fetch
 * @param <R> The result type after processing
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMarketDataOperation<T, R> {
    
    private static final String METRIC_PREFIX = "market.data.";
    private static final String METRIC_SUCCESS_COUNT = METRIC_PREFIX + "success.count";
    private static final String METRIC_FAILURE_COUNT = METRIC_PREFIX + "failure.count";
    private static final String TAG_DATA_TYPE = "data.type";
    
    protected final DataFetcher dataFetcher;
    protected final DataValidator<T> validator;
    protected final DataProcessor<T, R> processor;
    protected final MeterRegistry meterRegistry;
    protected final Executor executor;
    
    private String indexSymbol;
    
    /**
     * Set the index symbol for this operation
     * 
     * @param indexSymbol The index symbol to fetch data for
     * @return This operation instance for method chaining
     */
    public AbstractMarketDataOperation<T, R> withIndexSymbol(String indexSymbol) {
        this.indexSymbol = indexSymbol;
        return this;
    }
    
    /**
     * Get the current index symbol
     * 
     * @return The index symbol
     */
    protected String getIndexSymbol() {
        return indexSymbol;
    }
    
    /**
     * Execute the market data operation asynchronously
     * 
     * @return A CompletableFuture that will complete with true if the operation succeeded, false otherwise
     */
    public CompletableFuture<Boolean> executeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: Fetch data with retry
                T data = fetchData();
                
                if (data == null) {
                    log.warn("Failed to fetch {} data", getDataTypeName());
                    throw new DataValidationException(getIndexSymbol(), getDataTypeName(), "Failed to fetch data");
                }
                
                // Step 2: Validate data
                // if (!validator.isValid(data)) {
                //     log.warn("Skipping invalid or stale {} data", getDataTypeName());
                //     throw new DataValidationException(getIndexSymbol(), getDataTypeName(), "Invalid or stale data");
                // }
                
                R result = processor.process(data);
                
                // Step 4: Handle successful processing
                handleSuccess(result);
                
                meterRegistry.counter(METRIC_SUCCESS_COUNT, TAG_DATA_TYPE, getDataTypeName()).increment();
                return true;
                
            } catch (DataValidationException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to process {} data", getDataTypeName(), e);
                meterRegistry.counter(METRIC_FAILURE_COUNT, TAG_DATA_TYPE, getDataTypeName()).increment();
                throw new DataValidationException(getIndexSymbol(), getDataTypeName(), "Failed to process " + getDataTypeName() + " data", e);
            }
        }, executor);
    }
    
    /**
     * Get the data type name for logging and metrics
     * 
     * @return The data type name
     */
    protected abstract String getDataTypeName();
    
    /**
     * Fetch the data with retry
     * 
     * @return The fetched data
     */
    protected abstract T fetchData();
    
    /**
     * Handle successful processing
     * 
     * @param result The processed result
     */
    protected abstract void handleSuccess(R result);
}