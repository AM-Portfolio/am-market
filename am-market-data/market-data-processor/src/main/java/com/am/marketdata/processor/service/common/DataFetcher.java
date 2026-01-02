package com.am.marketdata.processor.service.common;

import com.am.marketdata.processor.exception.ProcessorException;
import com.am.marketdata.processor.exception.ProcessorException.ProcessorErrorType;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * Generic data fetcher with retry capabilities
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DataFetcher {
    
    private final MeterRegistry meterRegistry;
    private static final String METRIC_RETRY_COUNT = "market.data.retry.count";
    private static final String TAG_DATA_TYPE = "data.type";
    
    /**
     * Execute an operation with retry logic
     * 
     * @param operation The operation to execute
     * @param dataType The type of data being fetched (for metrics)
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelayMs Base delay between retries in milliseconds
     * @return The result of the operation
     * @throws MarketDataException if all retries fail
     */
    public <T> T executeWithRetry(Supplier<T> operation, String dataType, int maxRetries, long retryDelayMs) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                meterRegistry.counter(METRIC_RETRY_COUNT, TAG_DATA_TYPE, dataType).increment();
                
                if (attempt < maxRetries) {
                    long delay = calculateBackoffDelay(retryDelayMs, attempt);
                    log.warn("Attempt {} failed for {}, retrying in {} ms", attempt, dataType, delay, e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ProcessorException("Retry interrupted", ProcessorErrorType.RETRY_INTERRUPTED, "Retry interrupted", ie);
                    }
                }
            }
        }
        throw new ProcessorException("Operation failed after " + maxRetries + " retries", ProcessorErrorType.UNEXPECTED_ERROR, "Operation failed after " + maxRetries + " retries", lastException);
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private long calculateBackoffDelay(long baseDelay, int attempt) {
        return baseDelay * (long) Math.pow(2, attempt - 1);
    }
}