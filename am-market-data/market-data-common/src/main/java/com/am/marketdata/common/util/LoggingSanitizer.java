package com.am.marketdata.common.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility to sanitize sensitive information before logging.
 */
public final class LoggingSanitizer {

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "token", "access_token", "refresh_token", "request_token",
            "authorization", "cookie", "apiKey", "api-key", "secret",
            "secret_key", "password", "code", "client_secret"));

    private static final String MASK = "********";

    private LoggingSanitizer() {
    }

    /**
     * Sanitizes a map by masking sensitive keys.
     */
    public static Map<String, Object> sanitize(Map<String, Object> params) {
        if (params == null)
            return null;
        return params.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> isSensitive(e.getKey()) ? MASK : e.getValue()));
    }

    /**
     * Sanitizes a single key-value pair.
     */
    public static Object sanitize(String key, Object value) {
        if (isSensitive(key)) {
            return MASK;
        }
        return value;
    }

    /**
     * Masks sensitive query parameters in a URL or query string.
     */
    public static String maskQueryParams(String query) {
        if (query == null || query.isEmpty())
            return query;

        StringBuilder sb = new StringBuilder();
        String[] pairs = query.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String[] kv = pairs[i].split("=", 2);
            if (i > 0)
                sb.append("&");
            sb.append(kv[0]);
            if (kv.length > 1) {
                sb.append("=");
                sb.append(isSensitive(kv[0]) ? MASK : kv[1]);
            }
        }
        return sb.toString();
    }

    private static boolean isSensitive(String key) {
        if (key == null)
            return false;
        String normalized = key.toLowerCase().replace("_", "").replace("-", "");
        return SENSITIVE_KEYS.stream()
                .anyMatch(s -> normalized.contains(s.toLowerCase().replace("_", "").replace("-", "")));
    }
}
