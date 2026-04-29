package com.am.marketdata.api.filter;

import com.am.logging.AMLogger;
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

    private final AMLogger amLogger;

    public ApiLoggingFilter(AMLogger amLogger) {
        this.amLogger = amLogger;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> context = new HashMap<>();
            context.put("method", request.getMethod());
            context.put("uri", request.getRequestURI());
            context.put("status", response.getStatus());
            context.put("latency_ms", duration);
            context.put("client_ip", request.getRemoteAddr());
            context.put("query_params", request.getQueryString());

            amLogger.log("INFO", "API Request Processed", context);
        }
    }
}
