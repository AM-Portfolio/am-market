package com.am.marketdata.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LocationAwareLogger;

import java.util.UUID;

/**
 * Common AppLogger for standardized logging with correlation IDs.
 * Wraps SLF4J Logger and ensures correct caller detection using
 * LocationAwareLogger.
 */
public class AppLogger {

    private final Logger log;

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String FQCN = AppLogger.class.getName();

    // Private constructor to force usage of static factory or helper instance
    private AppLogger(Class<?> clazz) {
        this.log = LoggerFactory.getLogger(clazz);
    }

    /**
     * Get an instance of AppLogger for a specific class.
     * 
     * @param clazz The class to log for.
     * @return AppLogger instance.
     */
    public static AppLogger getLogger(Class<?> clazz) {
        return new AppLogger(clazz);
    }

    /**
     * Auto-detects the caller class and returns an AppLogger instance.
     * Uses StackWalker (Java 9+).
     * 
     * @return AppLogger instance for the caller.
     */
    public static AppLogger getLogger() {
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass();
        return new AppLogger(caller);
    }

    // --- Logging Methods ---

    /**
     * Log info with manual method name (Deprecated: Prefer letting logger detect
     * method)
     */
    public void info(String methodName, String message, Object... args) {
        log(LocationAwareLogger.INFO_INT, methodName, message, args, null);
    }

    public void info(String message, Object... args) {
        log(LocationAwareLogger.INFO_INT, null, message, args, null);
    }

    public void warn(String methodName, String message, Object... args) {
        log(LocationAwareLogger.WARN_INT, methodName, message, args, null);
    }

    public void warn(String message, Object... args) {
        log(LocationAwareLogger.WARN_INT, null, message, args, null);
    }

    public void error(String methodName, String message, Throwable t) {
        log(LocationAwareLogger.ERROR_INT, methodName, message, null, t);
    }

    public void error(String message, Throwable t) {
        log(LocationAwareLogger.ERROR_INT, null, message, null, t);
    }

    public void error(String methodName, String message, Object... args) {
        log(LocationAwareLogger.ERROR_INT, methodName, message, args, null);
    }

    public void error(String message, Object... args) {
        log(LocationAwareLogger.ERROR_INT, null, message, args, null);
    }

    public void debug(String methodName, String message, Object... args) {
        log(LocationAwareLogger.DEBUG_INT, methodName, message, args, null);
    }

    public void debug(String message, Object... args) {
        log(LocationAwareLogger.DEBUG_INT, null, message, args, null);
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    // --- Helper Methods ---

    private void log(int level, String methodName, String message, Object[] args, Throwable t) {
        ensureCorrelationId();

        // If methodName is provided, maybe append it to message or rely on Pattern?
        // Pattern will show ACTUAL method. If methodName != actual, we should preserve
        // it in message.
        String finalMessage = message;
        if (methodName != null && !methodName.isEmpty()) {
            finalMessage = String.format("[%s] %s", methodName, message);
        }

        if (log instanceof LocationAwareLogger) {
            ((LocationAwareLogger) log).log(null, FQCN, level, finalMessage, args, t);
        } else {
            // Fallback for non-LocationAware loggers
            switch (level) {
                case LocationAwareLogger.INFO_INT -> log.info(finalMessage, args);
                case LocationAwareLogger.WARN_INT -> log.warn(finalMessage, args);
                case LocationAwareLogger.ERROR_INT -> {
                    if (t != null)
                        log.error(finalMessage, t);
                    else
                        log.error(finalMessage, args);
                }
                case LocationAwareLogger.DEBUG_INT -> log.debug(finalMessage, args);
            }
        }
    }

    /**
     * Ensures a correlation ID exists in the MDC.
     * If not, generates a new one.
     */
    private void ensureCorrelationId() {
        if (MDC.get(CORRELATION_ID_KEY) == null) {
            MDC.put(CORRELATION_ID_KEY, UUID.randomUUID().toString());
        }
    }

    public static void clearMDC() {
        MDC.remove(CORRELATION_ID_KEY);
    }

    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }
}
