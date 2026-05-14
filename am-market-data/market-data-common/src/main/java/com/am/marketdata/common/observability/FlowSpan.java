package com.am.marketdata.common.observability;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a logical step in a flow. Captures start time and MDC state.
 * Implements AutoCloseable for use in try-with-resources.
 */
public final class FlowSpan implements AutoCloseable {

    private final FlowLogger logger;
    private final String stepName;
    private final long startNanos;
    private final Map<String, String> priorMdc;
    private final Map<String, String> initialFields;
    private boolean logged = false;

    FlowSpan(FlowLogger logger, String stepName, long startNanos,
            Map<String, String> priorMdc, Map<String, String> initialFields) {
        this.logger = logger;
        this.stepName = stepName;
        this.startNanos = startNanos;
        this.priorMdc = priorMdc;
        this.initialFields = initialFields;
    }

    @Override
    public void close() {
        // Restore prior MDC
        priorMdc.forEach((k, v) -> {
            if (v == null)
                MDC.remove(k);
            else
                MDC.put(k, v);
        });
    }

    public long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    public FlowSpan addAttribute(String key, Object value) {
        if (value != null) {
            initialFields.put(key, value.toString());
            MDC.put(key, value.toString());
        }
        return this;
    }

    public String stepName() {
        return stepName;
    }

    public Map<String, String> initialFields() {
        return initialFields;
    }

    public boolean isLogged() {
        return logged;
    }

    void markLogged() {
        this.logged = true;
    }

    static Map<String, String> snapshotMdc(String... keys) {
        Map<String, String> snapshot = new HashMap<>();
        for (String key : keys) {
            snapshot.put(key, MDC.get(key));
        }
        return snapshot;
    }
}
