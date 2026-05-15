package com.am.marketdata.api.filter;

import com.am.logging.AMLogger;
import com.am.observability.mdc.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final String SESSION_ID = "sessionId";
    private final AMLogger amLogger;

    public ApiLoggingFilter(AMLogger amLogger) {
        this.amLogger = amLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        // Skip noisy paths
        if (uri.contains("/actuator") || uri.contains("/health") || uri.contains("/swagger")
                || uri.contains("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        // Correlation ID setup
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = request.getHeader("X-Request-Id");
        }
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = java.util.UUID.randomUUID().toString();
        }

        // Snapshot MDC for later restoration
        Map<String, String> priorMdc = org.slf4j.MDC.getCopyOfContextMap();

        org.slf4j.MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        org.slf4j.MDC.put(MdcKeys.TRACE_ID, correlationId);

        // Safe Session ID (truncated to prevent sensitive leakage)
        String rawSessionId = request.getRequestedSessionId();
        if (rawSessionId != null) {
            String safeSessionId = rawSessionId.length() > 8 ? rawSessionId.substring(0, 8) + "..." : rawSessionId;
            org.slf4j.MDC.put(SESSION_ID, safeSessionId);
        }

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            org.slf4j.MDC.put("exception", e.getClass().getSimpleName());
            org.slf4j.MDC.put("exception_message", e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> context = new HashMap<>();
            context.put("method", request.getMethod());
            context.put("uri", uri);
            context.put("status", response.getStatus());
            context.put("latency_ms", duration);
            context.put("client_ip", request.getRemoteAddr());
            context.put("correlation_id", correlationId);
            
            String safeSessionId = org.slf4j.MDC.get(SESSION_ID);
            if (safeSessionId != null) {
                context.put("session_id", safeSessionId);
            }

            // Mask sensitive query params
            String queryString = request.getQueryString();
            if (queryString != null) {
                context.put("query_params",
                        com.am.marketdata.common.util.LoggingSanitizer.maskQueryParams(queryString));
            }

            amLogger.log(response.getStatus() >= 400 ? "WARN" : "INFO", "API Request Processed", context);

            // Restore prior MDC instead of clearing everything
            if (priorMdc == null) {
                org.slf4j.MDC.clear();
            } else {
                org.slf4j.MDC.setContextMap(priorMdc);
            }
        }
    }
}
