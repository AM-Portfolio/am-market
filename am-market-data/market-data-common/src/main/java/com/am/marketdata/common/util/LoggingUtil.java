package com.am.marketdata.common.util;

import com.am.marketdata.common.log.AppLogger;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for standardized logging across the application.
 * Provides methods for logging method entry/exit, input/output parameters, and
 * exceptions.
 */
public class LoggingUtil {

    private static final String OPERATION_KEY = "operation";
    private static final String COMPONENT_KEY = "component";
    private static final String SYMBOL_KEY = "symbol";
    private static final String INTERVAL_KEY = "interval";
    private static final String CACHE_KEY = "cacheKey";

    /**
     * Log method entry with input parameters
     * 
     * @param logger      The SLF4J logger instance
     * @param methodName  The name of the method being entered
     * @param inputParams Map of input parameter names and values
     */
    public static void logMethodEntry(Logger logger, String methodName, Map<String, Object> inputParams) {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering method: {} with parameters: {}", methodName, inputParams);
        }
    }

    public static void logMethodEntry(AppLogger logger, String methodName, Map<String, Object> inputParams) {
        logger.debug(methodName, "Entering method with parameters: " + inputParams);
    }

    /**
     * Log method exit with output result
     * 
     * @param logger     The SLF4J logger instance
     * @param methodName The name of the method being exited
     * @param result     The result object (will be converted to string)
     */
    public static void logMethodExit(Logger logger, String methodName, Object result) {
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting method: {} with result: {}", methodName, result);
        }
    }

    public static void logMethodExit(AppLogger logger, String methodName, Object result) {
        logger.debug(methodName, "Exiting method with result: " + result);
    }

    /**
     * Log cache operation with key and value details
     * 
     * @param logger    The SLF4J logger instance
     * @param operation The cache operation (e.g., "PUT", "GET", "DELETE")
     * @param key       The cache key
     * @param value     The cache value (can be null for operations like GET or
     *                  DELETE)
     */
    public static void logCacheOperation(Logger logger, String operation, String key, Object value) {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(OPERATION_KEY, operation);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(CACHE_KEY, key)) {

            if (value != null) {
                logger.info("Cache {} operation - Key: {} Value: {}", operation, key, value);
            } else {
                logger.info("Cache {} operation - Key: {}", operation, key);
            }
        }
    }

    public static void logCacheOperation(AppLogger logger, String operation, String key, Object value) {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(OPERATION_KEY, operation);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(CACHE_KEY, key)) {

            String message = (value != null)
                    ? String.format("Cache %s operation - Key: %s Value: %s", operation, key, value)
                    : String.format("Cache %s operation - Key: %s", operation, key);

            logger.info(operation, message);
        }
    }

    /**
     * Log market data operation with details
     * 
     * @param logger    The SLF4J logger instance
     * @param component The component performing the operation
     * @param operation The operation being performed
     * @param symbol    The market symbol (optional)
     * @param interval  The time interval (optional)
     * @param details   Additional details about the operation
     */
    public static void logMarketDataOperation(Logger logger, String component, String operation,
            String symbol, String interval, String details) {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(COMPONENT_KEY, component);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(OPERATION_KEY, operation)) {

            if (symbol != null) {
                MDC.put(SYMBOL_KEY, symbol);
            }

            if (interval != null) {
                MDC.put(INTERVAL_KEY, interval);
            }

            logger.info("{} - {} - Symbol: {} Interval: {} - {}",
                    component, operation, symbol != null ? symbol : "N/A",
                    interval != null ? interval : "N/A", details);

            // Clean up MDC
            if (symbol != null) {
                MDC.remove(SYMBOL_KEY);
            }

            if (interval != null) {
                MDC.remove(INTERVAL_KEY);
            }
        }
    }

    public static void logMarketDataOperation(AppLogger logger, String component, String operation,
            String symbol, String interval, String details) {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(COMPONENT_KEY, component);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(OPERATION_KEY, operation)) {

            if (symbol != null) {
                MDC.put(SYMBOL_KEY, symbol);
            }

            if (interval != null) {
                MDC.put(INTERVAL_KEY, interval);
            }

            String message = String.format("%s - %s - Symbol: %s Interval: %s - %s",
                    component, operation, symbol != null ? symbol : "N/A",
                    interval != null ? interval : "N/A", details);

            logger.info(operation, message);

            // Clean up MDC
            if (symbol != null) {
                MDC.remove(SYMBOL_KEY);
            }

            if (interval != null) {
                MDC.remove(INTERVAL_KEY);
            }
        }
    }

    /**
     * Log exception with context
     * 
     * @param logger    The SLF4J logger instance
     * @param component The component where the exception occurred
     * @param operation The operation during which the exception occurred
     * @param message   A descriptive message about the exception
     * @param exception The exception object
     */
    public static void logException(Logger logger, String component, String operation,
            String message, Throwable exception) {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(COMPONENT_KEY, component);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(OPERATION_KEY, operation)) {

            logger.error("{} - {} - Error: {} - Exception: {}",
                    component, operation, message, exception.getMessage(), exception);
        }
    }

    public static void logException(AppLogger logger, String component, String operation,
            String message, Throwable exception) {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(COMPONENT_KEY, component);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(OPERATION_KEY, operation)) {

            String detailedMessage = String.format("%s - %s - Error: %s - Exception: %s",
                    component, operation, message, exception.getMessage());

            logger.error(operation, detailedMessage, exception);
        }
    }

    /**
     * Execute a function and log its execution including any exceptions
     * 
     * @param <T>       The return type of the function
     * @param logger    The SLF4J logger instance
     * @param component The component executing the function
     * @param operation The operation being performed
     * @param function  The function to execute
     * @return The result of the function execution
     * @throws Exception if the function throws an exception
     */
    public static <T> T executeAndLog(Logger logger, String component, String operation,
            Supplier<T> function) throws Exception {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(COMPONENT_KEY, component);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(OPERATION_KEY, operation)) {

            logger.debug("{} - {} - Starting execution", component, operation);

            T result;
            try {
                result = function.get();
            } catch (Exception e) {
                logger.error("{} - {} - Failed with exception: {}",
                        component, operation, e.getMessage(), e);
                throw e;
            }

            logger.debug("{} - {} - Completed successfully", component, operation);
            return result;
        }
    }

    public static <T> T executeAndLog(AppLogger logger, String component, String operation,
            Supplier<T> function) throws Exception {
        try (MDC.MDCCloseable mdc1 = MDC.putCloseable(COMPONENT_KEY, component);
                MDC.MDCCloseable mdc2 = MDC.putCloseable(OPERATION_KEY, operation)) {

            logger.debug(operation, component + " - Starting execution");

            T result;
            try {
                result = function.get();
            } catch (Exception e) {
                logger.error(operation, component + " - Failed with exception: " + e.getMessage(), e);
                throw e;
            }

            logger.debug(operation, component + " - Completed successfully");
            return result;
        }
    }

    /**
     * Format cache key-value pair for logging
     * 
     * @param key   The cache key
     * @param value The cache value
     * @return Formatted string with key and value
     */
    public static String formatCacheKeyValue(String key, Object value) {
        return String.format("%s=%s", key, value);
    }

    /**
     * Format OHLC data for logging
     * 
     * @param open  Opening price
     * @param high  High price
     * @param low   Low price
     * @param close Closing price
     * @return Formatted OHLC string
     */
    public static String formatOHLCData(double open, double high, double low, double close) {
        return String.format("O:%.2f,H:%.2f,L:%.2f,C:%.2f", open, high, low, close);
    }
}
