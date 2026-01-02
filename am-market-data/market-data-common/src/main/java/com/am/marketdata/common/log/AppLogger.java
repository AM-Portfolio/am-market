package com.am.marketdata.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Common AppLogger for standardized logging with correlation IDs.
 * Provides simplified logging methods that auto-detect caller class.
 */
public class AppLogger {

    private final Logger log;
    private final String className;
    private static final String CORRELATION_ID_KEY = "correlationId";

    // Private constructor to force usage of static factory or helper instance
    private AppLogger(Class<?> clazz) {
        this.log = LoggerFactory.getLogger(clazz);
        this.className = clazz.getSimpleName();
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

    public void info(String methodName, String message, Object... args) {
        ensureCorrelationId();
        log.info(formatMessage(methodName, message), args);
    }

    public void warn(String methodName, String message, Object... args) {
        ensureCorrelationId();
        log.warn(formatMessage(methodName, message), args);
    }

    public void error(String methodName, String message, Throwable t) {
        ensureCorrelationId();
        log.error(formatMessage(methodName, message), t);
    }

    public void error(String methodName, String message, Object... args) {
        ensureCorrelationId();
        log.error(formatMessage(methodName, message), args);
    }

    public void debug(String methodName, String message, Object... args) {
        ensureCorrelationId();
        log.debug(formatMessage(methodName, message), args);
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    // --- Helper Methods ---

    private String formatMessage(String methodName, String message) {
        return String.format("[%s.%s] %s", className, methodName, message);
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

    /**
     * Clears correlation ID from MDC.
     */
    public static void clearMDC() {
        MDC.remove(CORRELATION_ID_KEY);
    }

    /**
     * Sets a specific correlation ID.
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }
}
