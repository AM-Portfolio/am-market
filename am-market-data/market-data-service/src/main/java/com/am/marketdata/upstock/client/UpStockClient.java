package com.am.marketdata.upstock.client;

import com.am.marketdata.upstock.config.UpstoxConfig;
import com.am.marketdata.upstock.model.*;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpStockClient {
    private final UpstoxConfig upstoxConfig;
    private final StringRedisTemplate redisTemplate;

    private static final String BASE_URL = "https://api.upstox.com/v2";
    private static final String REDIS_KEY_ACCESS_TOKEN = "market-data:upstox:access_token";

    // Market Data APIs
    public MarketQuoteResponse getMarketQuotes(List<String> symbols) {
        String url = BASE_URL + "/market-quote/quotes";
        return executeGet(url, MarketQuoteResponse.class, "symbol", formatSymbols(symbols));
    }

    public MarketQuoteResponse getFullMarketQuotes(List<String> symbols) {
        String url = BASE_URL + "/market-quote/full";
        return executeGet(url, MarketQuoteResponse.class, "symbol", formatSymbols(symbols));
    }

    public OHLCResponse getOHLCData(List<String> symbols, String interval) {
        String url = BASE_URL + "/market-quote/ohlc";
        return executeGet(url, OHLCResponse.class, "symbol", formatSymbols(symbols), "interval", interval);
    }

    // Historical Data APIs
    public HistoricalDataResponse getHistoricalData(String symbol, String interval, String from, String to) {
        // Upstox V2 API format:
        // /historical-candle/{instrumentKey}/{interval}/{to_date}/{from_date}
        String url = BASE_URL + "/historical-candle/" + symbol + "/" + interval + "/" + to + "/" + from;
        return executeGet(url, HistoricalDataResponse.class);
    }

    private String getAccessToken() {
        try {
            String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
            if (cachedToken != null && !cachedToken.isEmpty()) {
                return cachedToken;
            }
        } catch (Exception e) {
            log.warn("Failed to get access token from Redis: {}", e.getMessage());
        }
        return upstoxConfig.getAccessToken();
    }

    private <T> T executeGet(String url, Class<T> responseType, String... queryParams) {
        log.info("=== Executing GET request to Upstox API ===");
        logRequest("GET", url, queryParams);

        try {
            var request = Unirest.get(url)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .header("Api-Version", "2.0")
                    .header("Content-Type", "application/json");

            // Add query parameters if present
            for (int i = 0; i < queryParams.length; i += 2) {
                request.queryString(queryParams[i], queryParams[i + 1]);
            }

            HttpResponse<T> response = request.asObject(responseType);
            log.info("Request successful. Status: {}", response.getStatus());
            // logResponse(response);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to execute GET request. URL: {}, Error: {}", url, e.getMessage(), e);
            throw e;
        }
    }

    private String formatSymbols(List<String> symbols) {
        String formattedSymbols = symbols.stream()
                .map(symbol -> symbol.replace(":", "|"))
                .collect(Collectors.joining(","));
        log.info("Formatted symbols: {}", formattedSymbols);
        return formattedSymbols;
    }

    private void logRequest(String method, String url, Object params) {
        StringBuilder curl = new StringBuilder()
                .append("curl -X ").append(method)
                .append(" '").append(url);

        if (params != null) {
            if (params instanceof String[]) {
                String[] queryParams = (String[]) params;
                if (queryParams.length > 0) {
                    curl.append("?");
                    for (int i = 0; i < queryParams.length; i += 2) {
                        if (i > 0)
                            curl.append("&");
                        curl.append(queryParams[i]).append("=").append(queryParams[i + 1]);
                    }
                }
            } else {
                curl.append("' -d '").append(params);
            }
        }

        curl.append("'")
                .append(" -H 'Authorization: Bearer ").append(getAccessToken()).append("'")
                .append(" -H 'Api-Version: 2.0'")
                .append(" -H 'Content-Type: application/json'");

        log.info("API Request: {}", curl.toString());
    }

    private <T> void logResponse(HttpResponse<T> response) {
        log.info("Response Status: {}", response.getStatus());
        log.info("Response Headers: {}", response.getHeaders());
        // log.info("Raw Response Body: {}", response.getRawBody());
        log.info("Parsed Response Body: {}", response.getBody());
        if (response.getBody() instanceof OHLCResponse) {
            OHLCResponse ohlcResponse = (OHLCResponse) response.getBody();
            if (ohlcResponse.getData() != null) {
                ohlcResponse.getData().forEach((key, value) -> {
                    log.info("Key: {}, Value: {}", key, value);
                });
            }
        }
    }
}