package com.am.marketdata.common.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight implementation of FlowLogger compatible with am-core-services
 * style.
 */
public final class FlowLogger {

    private static final Logger LOG = LoggerFactory.getLogger(FlowLogger.class);

    /**
     * Starts a new flow span.
     */
    public FlowSpan start(String stepName, Object... kv) {
        long startNanos = System.nanoTime();
        Map<String, String> prior = FlowSpan.snapshotMdc(MdcKeys.FLOW_STEP, MdcKeys.FLOW_ID, MdcKeys.FLOW_OUTCOME,
                MdcKeys.FLOW_DURATION_MS);
        Map<String, String> fields = toMap(kv);

        String flowId = MDC.get(MdcKeys.FLOW_ID);
        if (flowId == null || flowId.isEmpty()) {
            flowId = MDC.get(MdcKeys.CORRELATION_ID);
            if (flowId == null || flowId.isEmpty()) {
                flowId = UUID.randomUUID().toString();
            }
            MDC.put(MdcKeys.FLOW_ID, flowId);
        }
        MDC.put(MdcKeys.FLOW_STEP, stepName);

        FlowSpan span = new FlowSpan(this, stepName, startNanos, prior, fields);
        emit(stepName, "start", null, fields, null);
        return span;
    }

    public void complete(FlowSpan span, Object... kv) {
        if (span == null || span.isLogged())
            return;
        long elapsed = span.elapsedMillis();
        Map<String, String> fields = mergeFields(span.initialFields(), toMap(kv));
        emit(span.stepName(), "ok", elapsed, fields, null);
        span.markLogged();
    }

    public void fail(FlowSpan span, Throwable cause, Object... kv) {
        if (span == null || span.isLogged())
            return;
        long elapsed = span.elapsedMillis();
        Map<String, String> fields = mergeFields(span.initialFields(), toMap(kv));
        if (cause != null) {
            fields.put("error", cause.getClass().getSimpleName());
            String msg = cause.getMessage();
            if (msg != null) {
                fields.put("error.message", msg.length() > 256 ? msg.substring(0, 256) + "..." : msg);
            }
        }
        emit(span.stepName(), "err", elapsed, fields, cause);
        span.markLogged();
    }

    public void warn(FlowSpan span, String message, Object... kv) {
        if (span == null || span.isLogged())
            return;
        long elapsed = span.elapsedMillis();
        Map<String, String> fields = mergeFields(span.initialFields(), toMap(kv));
        if (message != null) {
            fields.put("warning", message);
        }
        emit(span.stepName(), "warn", elapsed, fields, null);
        span.markLogged();
    }

    private void emit(String stepName, String outcome, Long durationMs, Map<String, String> fields, Throwable cause) {
        try {
            MDC.put(MdcKeys.FLOW_OUTCOME, outcome);
            if (durationMs != null) {
                MDC.put(MdcKeys.FLOW_DURATION_MS, String.valueOf(durationMs));
            }

            String summary = renderSummary(stepName, outcome, durationMs, fields);
            if ("err".equals(outcome)) {
                if (cause != null)
                    LOG.error(FlowMarkers.FLOW, summary, cause);
                else
                    LOG.error(FlowMarkers.FLOW, summary);
            } else {
                LOG.info(FlowMarkers.FLOW, summary);
            }
        } finally {
            MDC.remove(MdcKeys.FLOW_OUTCOME);
            MDC.remove(MdcKeys.FLOW_DURATION_MS);
        }
    }

    private String renderSummary(String stepName, String outcome, Long durationMs, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[FLOW step=").append(stepName);
        String flowId = MDC.get(MdcKeys.FLOW_ID);
        if (flowId != null)
            sb.append(" flow=").append(flowId);
        
        String sessId = MDC.get(MdcKeys.SESSION_ID);
        if (sessId != null)
            sb.append(" session=").append(sessId);

        sb.append(" status=").append(outcome);
        if (durationMs != null)
            sb.append(" dur_ms=").append(durationMs);
        sb.append("]");

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static Map<String, String> toMap(Object... kv) {
        Map<String, String> out = new LinkedHashMap<>();
        if (kv == null)
            return out;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i] != null) {
                out.put(String.valueOf(kv[i]), kv[i + 1] == null ? "null" : String.valueOf(kv[i + 1]));
            }
        }
        return out;
    }

    private static Map<String, String> mergeFields(Map<String, String> base, Map<String, String> overlay) {
        Map<String, String> merged = new LinkedHashMap<>(base);
        merged.putAll(overlay);
        return merged;
    }
}
