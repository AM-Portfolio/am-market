package com.am.marketdata.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stops the service before it makes unauthenticated historical-price requests.
 * All values must arrive through Vault mappings; this class never logs them.
 */
@Component
public class InfluxConfigurationValidator {

    @Value("${spring.influx.url:}")
    private String url;

    @Value("${spring.influx.token:}")
    private String token;

    @Value("${spring.influx.org:}")
    private String org;

    @Value("${spring.influx.bucket:}")
    private String bucket;

    @PostConstruct
    void validate() {
        if (!hasUsableUrl() || isBlank(token) || isBlank(org) || isBlank(bucket)) {
            throw new IllegalStateException(
                    "Influx configuration is incomplete. Check Vault mappings for URL, token, org, and bucket.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasUsableUrl() {
        if (isBlank(url)) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            int port = uri.getPort();
            if (port <= 0) {
                if ("http".equalsIgnoreCase(scheme)) {
                    port = 80;
                } else if ("https".equalsIgnoreCase(scheme)) {
                    port = 443;
                }
            }
            return port > 0;
        } catch (URISyntaxException ignored) {
            return false;
        }
    }
}
