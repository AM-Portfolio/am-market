package com.am.marketdata.scraper.client;

import com.am.marketdata.common.model.NSEIndicesResponse;
import com.am.marketdata.common.model.NSEStockInsidicesData;
import com.am.marketdata.scraper.exception.NSEApiException;
import com.am.marketdata.scraper.cookie.CookieCache;
import com.am.marketdata.scraper.cookie.CookieManager;
import com.am.marketdata.scraper.exception.CookieException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class NSEApiClient {
    @Qualifier("nseApiRestTemplate")
    private final RestTemplate restTemplate;
    private final CookieCache cookieCache;
    private final CookieManager cookieManager;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${nse.api.base-url:https://www.nseindia.com}")
    private String baseUrl;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36";

    // Metric names
    private static final String METRIC_PREFIX = "nse.api.";
    private static final String METRIC_REQUEST_TIME = METRIC_PREFIX + "request.time";
    private static final String METRIC_ERROR_COUNT = METRIC_PREFIX + "error.count";
    private static final String METRIC_REQUEST_COUNT = METRIC_PREFIX + "request.count";
    private static final String TAG_ENDPOINT = "endpoint";
    private static final String TAG_ERROR_TYPE = "error_type";

    private Timer stockIndicesRequestTimer;
    private Timer indicesRequestTimer;

    @PostConstruct
    public void initialize() {
        // Initialize timers for each endpoint
        stockIndicesRequestTimer = Timer.builder(METRIC_REQUEST_TIME)
                .tag(TAG_ENDPOINT, "stock_indices")
                .description("Time taken for stock indices API requests")
                .register(meterRegistry);

        indicesRequestTimer = Timer.builder(METRIC_REQUEST_TIME)
                .tag(TAG_ENDPOINT, "indices")
                .description("Time taken for indices API requests")
                .register(meterRegistry);
    }

    public NSEStockInsidicesData getStockIndices(String indexSymbol) {
        String encodedSymbol = java.net.URLEncoder.encode(indexSymbol, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        return stockIndicesRequestTimer.record(() -> executeApiCall("/api/equity-stockIndices?index=" + encodedSymbol,
                NSEStockInsidicesData.class, this::logStockIndicesResponse));
    }

    public NSEIndicesResponse getAllIndices() {
        return indicesRequestTimer
                .record(() -> executeApiCall("/api/allIndices", NSEIndicesResponse.class, this::logIndicesResponse));
    }

    public JsonNode getIpoPastIssues() {
        return executeApiCall("/api/public-past-issues", JsonNode.class, body ->
                log.info("IPO past issues count={}", body == null || !body.isArray() ? 0 : body.size()));
    }

    public JsonNode getIpoCurrentIssues() {
        return executeApiCall("/api/ipo-current-issue", JsonNode.class, body ->
                log.info("IPO current issues count={}", body == null || !body.isArray() ? 0 : body.size()));
    }

    public JsonNode getIpoUpcomingIssues() {
        return executeApiCall("/api/all-upcoming-issues?category=ipo", JsonNode.class, body ->
                log.info("IPO upcoming issues count={}", body == null || !body.isArray() ? 0 : body.size()));
    }

    public JsonNode getIpoBidDetails(String symbol, String series) {
        String sym = java.net.URLEncoder.encode(symbol, java.nio.charset.StandardCharsets.UTF_8);
        String ser = java.net.URLEncoder.encode(series == null ? "EQ" : series, java.nio.charset.StandardCharsets.UTF_8);
        return executeApiCall("/api/ipo-bid-details?symbol=" + sym + "&series=" + ser, JsonNode.class, body ->
                log.info("IPO bid details symbol={} series={}", symbol, series));
    }

    private HttpHeaders createBasicHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private <T> T executeApiCall(String endpoint, Class<T> responseType, ResponseLogger<T> responseLogger) {
        String cookies = getCookiesOrThrow();
        String url = baseUrl + endpoint;
        HttpEntity<String> entity = createHttpEntity(cookies);

        // Record request count
        meterRegistry.counter(METRIC_REQUEST_COUNT, TAG_ENDPOINT, endpoint).increment();

        try {
            log.info("Calling NSE API - Endpoint: {}, Cookies: {}, Headers: {}",
                    endpoint,
                    maskCookieValues(cookies),
                    maskSensitiveHeaders(entity.getHeaders()));
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            if (response.getBody() == null) {
                recordError(endpoint, "empty_response");
                throw new NSEApiException(endpoint, response.getStatusCode(), "null", "Empty response from NSE API");
            }

            logApiResponse(endpoint, response, responseLogger);
            return response.getBody();

        } catch (CookieException e) {
            log.error("Cookie unavailable for NSE API - Endpoint: {}, Error: {}", endpoint, e.getMessage());
            recordError(endpoint, "cookie_error");
            throw e;
        } catch (HttpClientErrorException.Unauthorized e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Unauthorized access to NSE API - Endpoint: {}, Response: {}, Headers: {}",
                    endpoint, responseBody, maskSensitiveHeaders(e.getResponseHeaders()));
            cookieCache.invalidateCookies();
            recordError(endpoint, "unauthorized");
            throw new NSEApiException(endpoint, HttpStatus.UNAUTHORIZED, responseBody,
                    "Unauthorized access, cookies might be expired", e);

        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Client error from NSE API - Endpoint: {}, Status: {}, Response: {}, Headers: {}",
                    endpoint, e.getStatusCode(), responseBody, maskSensitiveHeaders(e.getResponseHeaders()));
            recordError(endpoint, "client_error");
            throw new NSEApiException(endpoint, e.getStatusCode(), responseBody, "Client error from NSE API", e);

        } catch (HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Server error from NSE API - Endpoint: {}, Status: {}, Response: {}, Headers: {}",
                    endpoint, e.getStatusCode(), responseBody, maskSensitiveHeaders(e.getResponseHeaders()));
            recordError(endpoint, "server_error");
            throw new NSEApiException(endpoint, e.getStatusCode(), responseBody, "Server error from NSE API", e);

        } catch (ResourceAccessException e) {
            log.error("Network error calling NSE API - Endpoint: {}, Error: {}", endpoint, e.getMessage());
            recordError(endpoint, "network_error");
            throw new NSEApiException(endpoint, HttpStatus.SERVICE_UNAVAILABLE, "N/A",
                    "Network error accessing NSE API", e);

        } catch (Exception e) {
            log.error("Unexpected error calling NSE API - Endpoint: {}, Error: {}", endpoint, e.getMessage(), e);
            recordError(endpoint, "unexpected_error");
            throw new NSEApiException(endpoint, HttpStatus.INTERNAL_SERVER_ERROR, "N/A",
                    "Unexpected error calling NSE API", e);
        }
    }

    private void recordError(String endpoint, String errorType) {
        meterRegistry.counter(METRIC_ERROR_COUNT,
                TAG_ENDPOINT, endpoint,
                TAG_ERROR_TYPE, errorType).increment();
    }

    private String getCookiesOrThrow() {
        return cookieManager.getCookiesForApi();
    }

    private HttpEntity<String> createHttpEntity(String cookies) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, cookies);
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.REFERER, baseUrl + "/");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
        return new HttpEntity<>(headers);
    }

    private HttpHeaders maskSensitiveHeaders(HttpHeaders headers) {
        if (headers == null)
            return null;
        HttpHeaders masked = new HttpHeaders();
        headers.forEach((key, value) -> {
            if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) {
                masked.put(key, Collections.singletonList("*****"));
            } else {
                masked.put(key, value);
            }
        });
        return masked;
    }

    private String maskCookieValues(String cookies) {
        if (cookies == null)
            return null;
        // Split cookies and mask values while preserving names
        return Stream.of(cookies.split(";"))
                .map(cookie -> {
                    String[] parts = cookie.split("=", 2);
                    return parts.length > 1
                            ? parts[0].trim() + "=*****"
                            : cookie.trim() + "=*****";
                })
                .collect(Collectors.joining("; "));
    }

    private <T> void logApiResponse(String endpoint, ResponseEntity<T> response, ResponseLogger<T> responseLogger) {
        try {
            log.info("NSE API Response - Endpoint: {}, Status: {}, Headers: {}",
                    endpoint, response.getStatusCode(), maskSensitiveHeaders(response.getHeaders()));

            if (response.getBody() != null) {
                responseLogger.log(response.getBody());
            }
        } catch (Exception e) {
            log.warn("Failed to log API response details - Endpoint: {}", endpoint, e);
        }
    }

    @FunctionalInterface
    private interface ResponseLogger<T> {
        void log(T response) throws Exception;
    }

    private void logStockIndicesResponse(NSEStockInsidicesData stockIndices) throws Exception {
        log.debug("Stock Indices Response - Raw: {}", objectMapper.writeValueAsString(stockIndices));
        if (stockIndices.getData() != null) {
            log.info("Stock Indices Summary - Count: {}, First Stock Index: {}",
                    stockIndices.getData().size(),
                    stockIndices.getData().isEmpty() ? "none" : stockIndices.getData().get(0).getSymbol());
        }
    }

    private void logIndicesResponse(NSEIndicesResponse indices) throws Exception {
        log.debug("Indices Response - Raw: {}", objectMapper.writeValueAsString(indices));
        if (indices.getData() != null) {
            log.info("Indices Summary - Count: {}, First Index: {}",
                    indices.getData().size(),
                    indices.getData().isEmpty() ? "none"
                            : String.format("%s (Last: %.2f, Change: %.2f%%)",
                                    indices.getData().get(0).getIndexSymbol(),
                                    indices.getData().get(0).getLast(),
                                    indices.getData().get(0).getPercentChange()));
        }
    }
}
