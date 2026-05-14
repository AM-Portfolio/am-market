package com.am.marketdata.common.observability;

/**
 * Standard MDC keys used for structured logging and context propagation.
 */
public final class MdcKeys {
    public static final String CORRELATION_ID = "correlationId";
    public static final String TRACE_ID = "traceId";
    public static final String SESSION_ID = "sessionId";
    public static final String USER_ID = "userId";
    
    // Flow logging specific keys
    public static final String FLOW_ID = "flow.id";
    public static final String FLOW_STEP = "flow.step";
    public static final String FLOW_USER = "flow.user";
    public static final String FLOW_OUTCOME = "flow.outcome";
    public static final String FLOW_DURATION_MS = "flow.dur_ms";

    private MdcKeys() {}
}
