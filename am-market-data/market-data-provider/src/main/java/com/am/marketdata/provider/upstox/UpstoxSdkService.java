package com.am.marketdata.provider.upstox;

import com.am.marketdata.provider.upstox.config.UpstoxConfig;
import com.upstox.ApiClient;
import com.upstox.ApiException;
import com.upstox.auth.OAuth;
import io.swagger.client.api.MarketQuoteV3Api;
import io.swagger.client.api.HistoryV3Api;
import com.upstox.api.GetMarketQuoteLastTradedPriceResponseV3;
import com.upstox.api.GetHistoricalCandleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
public class UpstoxSdkService {

    private final StringRedisTemplate redisTemplate;
    private final UpstoxConfig upstoxConfig;
    private String accessToken;

    private static final String REDIS_KEY_ACCESS_TOKEN = "market_data:upstox:access_token";

    public UpstoxSdkService(StringRedisTemplate redisTemplate, UpstoxConfig upstoxConfig) {
        this.redisTemplate = redisTemplate;
        this.upstoxConfig = upstoxConfig;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Upstox SDK Service");
        try {
            // Try to load cached token from Redis
            String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
            if (cachedToken != null && !cachedToken.isEmpty()) {
                log.info("Found cached Access Token in Redis for SDK Service");
                this.setAccessToken(sanitizeAccessToken(cachedToken));
            } else {
                log.info("No cached Access Token found in Redis for SDK Service, checking configuration");
                if (upstoxConfig.getAccessToken() != null && !upstoxConfig.getAccessToken().isEmpty()) {
                    log.info("Found Access Token in configuration for SDK Service");
                    this.setAccessToken(sanitizeAccessToken(upstoxConfig.getAccessToken()));
                } else {
                    log.warn("No Access Token found for SDK Service");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to initialize SDK Service token from Redis: {}", e.getMessage());
            // Fallback to config
            if (upstoxConfig.getAccessToken() != null && !upstoxConfig.getAccessToken().isEmpty()) {
                this.setAccessToken(upstoxConfig.getAccessToken());
            }
        }
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = sanitizeAccessToken(accessToken);
    }

    private String getDynamicAccessToken() {
        if (this.accessToken != null && !this.accessToken.isEmpty()) {
            return this.accessToken;
        }
        
        try {
            // 1. Try to read from Redis cache first
            String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
            
            if (cachedToken != null && !cachedToken.isEmpty()) {
                // Check remaining TTL in Redis
                Long remainingTtlSeconds = redisTemplate.getExpire(REDIS_KEY_ACCESS_TOKEN, java.util.concurrent.TimeUnit.SECONDS);
                
                // If it has less than 24 hours left (86400 seconds), trigger a warm-up refresh from Vault config
                if (remainingTtlSeconds != null && remainingTtlSeconds > 0 && remainingTtlSeconds < 86400) {
                    log.info("getDynamicAccessToken", "Access token is nearing expiration (< 24 hours left). Refreshing from Vault configuration.");
                    String freshToken = upstoxConfig.getAccessToken();
                    if (freshToken != null && !freshToken.isEmpty()) {
                        String sanitized = sanitizeAccessToken(freshToken);
                        redisTemplate.opsForValue().set(REDIS_KEY_ACCESS_TOKEN, sanitized, 7, java.util.concurrent.TimeUnit.DAYS);
                        this.accessToken = sanitized;
                        return this.accessToken;
                    }
                }
                
                this.accessToken = sanitizeAccessToken(cachedToken);
                return this.accessToken;
            }
        } catch (Exception e) {
            log.warn("getDynamicAccessToken", "Failed to get or verify access token TTL in Redis: " + e.getMessage());
        }
        
        // 2. Fallback to Vault configuration (Cache-Aside DB fallback)
        if (upstoxConfig.getAccessToken() != null && !upstoxConfig.getAccessToken().isEmpty()) {
            String sanitized = sanitizeAccessToken(upstoxConfig.getAccessToken());
            try {
                // 3. Self-heal: Cache it back in Redis for 7 days
                redisTemplate.opsForValue().set(REDIS_KEY_ACCESS_TOKEN, sanitized, 7, java.util.concurrent.TimeUnit.DAYS);
                log.info("getDynamicAccessToken", "Successfully cached Vault Upstox Access Token in Redis for 7 days (Self-healed)");
            } catch (Exception e) {
                log.warn("getDynamicAccessToken", "Failed to write self-healed token to Redis: " + e.getMessage());
            }
            this.accessToken = sanitized;
        }
        return this.accessToken;
    }

    /** Strip accidental JSON suffixes from env vars (e.g. {@code ...","extended_token":"...}). */
    static String sanitizeAccessToken(String accessToken) {
        if (accessToken == null) {
            return null;
        }
        String token = accessToken.trim();
        int jsonSuffix = token.indexOf("\",\"");
        if (jsonSuffix > 0) {
            token = token.substring(0, jsonSuffix);
        }
        if (token.startsWith("\"")) {
            token = token.substring(1);
        }
        int closingQuote = token.indexOf('"');
        if (closingQuote > 0) {
            token = token.substring(0, closingQuote);
        }
        return token.trim();
    }

    /**
     * Fetch LTP for a list of instrument keys using Upstox V3 SDK
     * (MarketQuoteV3Api)
     *
     * @param instrumentKeys List of instrument keys (e.g., "NSE_EQ|INE848E01016")
     * @return GetMarketQuoteLastTradedPriceResponseV3 containing LTP data
     * @throws ApiException if the API call fails
     */
    public GetMarketQuoteLastTradedPriceResponseV3 getLtp(List<String> instrumentKeys)
            throws ApiException {
        String token = getDynamicAccessToken();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Upstox Access token is not initialized");
        }

        if (instrumentKeys == null || instrumentKeys.isEmpty()) {
            return new GetMarketQuoteLastTradedPriceResponseV3();
        }

        // Initialize ApiClient
        ApiClient apiClient = new ApiClient();

        // Configure OAuth2 access token
        // Use the auth name "OAUTH2" as per standard generated SDKs
        OAuth oAuth = (OAuth) apiClient.getAuthentication("OAUTH2");
        if (oAuth != null) {
            oAuth.setAccessToken(this.accessToken);
        } else {
            // Fallback if getAuthentication returns null or name differs (though OAUTH2 is
            // standard)
            // Some SDK versions might allow setAccessToken directly on client
            apiClient.setAccessToken(this.accessToken);
        }

        MarketQuoteV3Api marketQuoteV3Api = new MarketQuoteV3Api(apiClient);

        // Normalize keys replacing colon with pipe
        List<String> normalizedKeys = new java.util.ArrayList<>();
        for (String key : instrumentKeys) {
            normalizedKeys.add(key != null ? key.replace(":", "|") : null);
        }
        String symbolList = String.join(",", normalizedKeys);

        log.debug("Calling MarketQuoteV3Api.getLtp with symbols: {}", symbolList);
        return marketQuoteV3Api.getLtp(symbolList);
    }

    /**
     * Fetch OHLC for a list of instrument keys using Upstox V3 SDK
     * (MarketQuoteV3Api)
     *
     * @param instrumentKeys List of instrument keys (e.g., "NSE_EQ|INE848E01016")
     * @param interval       OHLC interval (e.g., "1minute", "day")
     * @return OHLCResponse containing mapped OHLC data
     * @throws ApiException if the API call fails
     */
    public com.am.marketdata.provider.upstox.model.OHLCResponse getOhlc(List<String> instrumentKeys, String interval)
            throws ApiException {
        String token = getDynamicAccessToken();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Upstox Access token is not initialized");
        }

        if (instrumentKeys == null || instrumentKeys.isEmpty()) {
            return new com.am.marketdata.provider.upstox.model.OHLCResponse();
        }

        // Initialize ApiClient
        ApiClient apiClient = new ApiClient();

        // Configure OAuth2 access token
        OAuth oAuth = (OAuth) apiClient.getAuthentication("OAUTH2");
        if (oAuth != null) {
            oAuth.setAccessToken(this.accessToken);
        } else {
            apiClient.setAccessToken(this.accessToken);
        }

        MarketQuoteV3Api marketQuoteV3Api = new MarketQuoteV3Api(apiClient);

        // Normalize keys replacing colon with pipe
        List<String> normalizedKeys = new java.util.ArrayList<>();
        for (String key : instrumentKeys) {
            normalizedKeys.add(key != null ? key.replace(":", "|") : null);
        }
        String symbolList = String.join(",", normalizedKeys);

        log.debug("Calling MarketQuoteV3Api.getOHLC with symbols: {} and interval: {}", symbolList, interval);
        log.debug("Access Token (masked): {}...",
                this.accessToken != null && this.accessToken.length() > 10 ? this.accessToken.substring(0, 10)
                        : "null");

        com.upstox.api.GetMarketQuoteOHLCResponseV3 sdkResponse;
        try {
            sdkResponse = marketQuoteV3Api.getMarketQuoteOHLC(interval, symbolList);
            log.info("Upstox SDK Response received for {} symbols", instrumentKeys.size());
        } catch (ApiException e) {
            log.error("Upstox SDK API Exception: Code={}, Body={}, Headers={}", e.getCode(), e.getResponseBody(),
                    e.getResponseHeaders());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in Upstox SDK getOhlc", e);
            throw e;
        }

        return mapToOHLCResponse(sdkResponse);
    }

    private com.am.marketdata.provider.upstox.model.OHLCResponse mapToOHLCResponse(
            com.upstox.api.GetMarketQuoteOHLCResponseV3 sdkResponse) {
        com.am.marketdata.provider.upstox.model.OHLCResponse response = new com.am.marketdata.provider.upstox.model.OHLCResponse();
        response.setStatus(sdkResponse.getStatus().toString());

        java.util.Map<String, com.am.marketdata.provider.upstox.model.OHLCResponse.OHLCData> dataMap = new java.util.HashMap<>();

        if (sdkResponse.getData() != null) {
            for (java.util.Map.Entry<String, com.upstox.api.MarketQuoteOHLCV3> entry : sdkResponse.getData()
                    .entrySet()) {
                dataMap.put(entry.getKey(), mapToOHLCData(entry.getValue()));
            }
        }

        response.setData(dataMap);
        return response;
    }

    private com.am.marketdata.provider.upstox.model.OHLCResponse.OHLCData mapToOHLCData(
            com.upstox.api.MarketQuoteOHLCV3 sdkData) {
        com.am.marketdata.provider.upstox.model.OHLCResponse.OHLCData ohlcData = new com.am.marketdata.provider.upstox.model.OHLCResponse.OHLCData();

        // Map generic fields
        ohlcData.setLast_price(sdkData.getLastPrice());
        ohlcData.setInstrument_token(sdkData.getInstrumentToken());

        // Map Live OHLC values
        if (sdkData.getLiveOhlc() != null) {
            com.am.marketdata.provider.upstox.model.OHLCResponse.OHLC ohlc = new com.am.marketdata.provider.upstox.model.OHLCResponse.OHLC();
            ohlc.setOpen(sdkData.getLiveOhlc().getOpen());
            ohlc.setHigh(sdkData.getLiveOhlc().getHigh());
            ohlc.setLow(sdkData.getLiveOhlc().getLow());
            ohlc.setClose(sdkData.getLiveOhlc().getClose());
            ohlcData.setOhlc(ohlc);
        }

        // Map Previous Close from PrevOHLC
        if (sdkData.getPrevOhlc() != null) {
            log.debug("Found PrevOHLC from SDK for token {}: {}", sdkData.getInstrumentToken(),
                    sdkData.getPrevOhlc().getClose());
            ohlcData.setPrevious_close(sdkData.getPrevOhlc().getClose());
        } else {
            log.debug("PrevOHLC is NULL from SDK for token: {}", sdkData.getInstrumentToken());
        }

        return ohlcData;
    }

    /**
     * Get historical candle data via Upstox V3 SDK.
     * <p>
     * Uses {@code GET /v3/historical-candle/{key}/{unit}/{interval}/{to_date}} when
     * {@code fromDate} is blank, otherwise
     * {@code .../{to_date}/{from_date}}.
     * Unit must be {@code minutes}, {@code days}, etc. (see API doc).
     */
    public com.am.marketdata.provider.upstox.model.HistoricalDataResponse getHistoricalCandleData(String instrumentKey,
            String unit, Integer interval, String toDate, String fromDate) {
        String token = getDynamicAccessToken();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Upstox Access token is not initialized");
        }

        try {
            ApiClient apiClient = new ApiClient();
            OAuth oAuth = (OAuth) apiClient.getAuthentication("OAUTH2");
            if (oAuth != null) {
                oAuth.setAccessToken(this.accessToken);
            } else {
                apiClient.setAccessToken(this.accessToken);
            }

            HistoryV3Api historyV3Api = new HistoryV3Api(apiClient);
            boolean useDateRange = fromDate != null && !fromDate.isBlank() && !fromDate.equals(toDate);

            String normalizedKey = instrumentKey != null ? instrumentKey.replace(":", "|") : null;

            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
            boolean isQueryingToday = toDate != null && !java.time.LocalDate.parse(toDate).isBefore(today);

            // Check if symbol is an Index. If so, we bypass the buggy SDK client to prevent URL-encoding issues (space to + instead of %20)
            boolean isIndex = normalizedKey != null && (normalizedKey.startsWith("NSE_INDEX") || normalizedKey.contains("INDEX"));

            if (isIndex) {
                return fetchHistoricalCandleDirect(normalizedKey, unit, interval, toDate, fromDate, isQueryingToday);
            }

            if (isQueryingToday && "minutes".equalsIgnoreCase(unit)) {
                // The Upstox Swagger client does not URL-encode the path parameter for the intraday endpoint.
                // We must manually URL-encode normalizedKey (e.g. replacing ' ' with '%20' and '|' with '%7C') to prevent HTTP 400.
                String encodedIntradayKey = normalizedKey;
                try {
                    encodedIntradayKey = java.net.URLEncoder.encode(normalizedKey, java.nio.charset.StandardCharsets.UTF_8.toString())
                            .replace("+", "%20"); // Upstox expects %20 instead of + for spaces
                } catch (java.io.UnsupportedEncodingException uee) {
                    log.error("getHistoricalCandleData", "Failed to URL-encode intraday key: " + normalizedKey, uee);
                }

                log.info(
                        "Fetching live intraday data key={}, interval=1minute",
                        encodedIntradayKey);
                com.upstox.api.GetIntraDayCandleResponse intradayResponse = historyV3Api.getIntraDayCandleData(
                        encodedIntradayKey, "1minute", 2);
                return mapIntradayToHistoricalDataResponse(intradayResponse);
            }

            GetHistoricalCandleResponse sdkResponse;
            if (useDateRange) {
                log.info(
                        "Fetching historical data (range) key={}, unit={}, interval={}, to={}, from={}",
                        normalizedKey, unit, interval, toDate, fromDate);
                sdkResponse = historyV3Api.getHistoricalCandleData1(
                        normalizedKey, unit, interval, toDate, fromDate);
            } else {
                log.info(
                        "Fetching historical data (to_date only) key={}, unit={}, interval={}, to={}",
                        normalizedKey, unit, interval, toDate);
                sdkResponse = historyV3Api.getHistoricalCandleData(
                        normalizedKey, unit, interval, toDate);
            }

            return mapToHistoricalDataResponse(sdkResponse);
        } catch (Exception e) {
            log.error("Error getting historical candle data from SDK", e);
            throw new RuntimeException("Error getting historical candle data", e);
        }
    }

    /**
     * Bypasses the Upstox SDK to directly query the REST API for index symbols.
     * This avoids SDK path parameter encoding issues where spaces are encoded as '+' instead of '%20'.
     */
    private com.am.marketdata.provider.upstox.model.HistoricalDataResponse fetchHistoricalCandleDirect(
            String normalizedKey, String unit, Integer interval, String toDate, String fromDate, boolean isQueryingToday) {
        try {
            String token = getDynamicAccessToken();
            // Safeguard against double-encoding (decodes the key to plain text first, then encodes it cleanly exactly once)
            String decodedKey = java.net.URLDecoder.decode(normalizedKey, java.nio.charset.StandardCharsets.UTF_8.toString());
            String encodedKey = java.net.URLEncoder.encode(decodedKey, java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");

            String urlStr;
            if (isQueryingToday && "minutes".equalsIgnoreCase(unit)) {
                urlStr = String.format("https://api.upstox.com/v2/historical-candle/intraday/%s/1minute", encodedKey);
            } else {
                String tf = "day".equalsIgnoreCase(unit) ? "day" : (interval + unit); // e.g. 1minute, 30minute, day
                if (fromDate != null && !fromDate.isBlank() && !fromDate.equals(toDate)) {
                    urlStr = String.format("https://api.upstox.com/v2/historical-candle/%s/%s/%s/%s", encodedKey, tf, toDate, fromDate);
                } else {
                    urlStr = String.format("https://api.upstox.com/v2/historical-candle/%s/%s/%s", encodedKey, tf, toDate);
                }
            }

            log.info("Directly fetching historical data from URL: {}", urlStr);

            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Deserialize JSON response manually
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(response.toString());

                com.am.marketdata.provider.upstox.model.HistoricalDataResponse res = new com.am.marketdata.provider.upstox.model.HistoricalDataResponse();
                res.setStatus(rootNode.path("status").asText());

                com.fasterxml.jackson.databind.JsonNode candlesNode = rootNode.path("data").path("candles");
                List<List<Object>> candlesList = new java.util.ArrayList<>();
                if (candlesNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode candle : candlesNode) {
                        List<Object> candleData = new java.util.ArrayList<>();
                        for (com.fasterxml.jackson.databind.JsonNode val : candle) {
                            if (val.isNumber()) {
                                candleData.add(val.doubleValue());
                            } else {
                                candleData.add(val.asText());
                            }
                        }
                        candlesList.add(candleData);
                    }
                }

                // FALLBACK: If querying today's intraday returned 0 candles (e.g. Nifty 500 is not supported on live endpoint),
                // fallback to querying the historical range endpoint for today's date.
                if (candlesList.isEmpty() && isQueryingToday && "minutes".equalsIgnoreCase(unit)) {
                    log.info("Intraday endpoint returned 0 candles for index: {}. Falling back to historical range endpoint for date: {}", normalizedKey, toDate);
                    return fetchHistoricalCandleDirect(normalizedKey, unit, interval, toDate, toDate, false);
                }

                com.am.marketdata.provider.upstox.model.HistoricalDataResponse.DataPayload dataPayload = 
                        new com.am.marketdata.provider.upstox.model.HistoricalDataResponse.DataPayload();
                dataPayload.setCandles(candlesList);
                res.setData(dataPayload);
                return res;
            } else {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()));
                String inputLine;
                StringBuilder errorResponse = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    errorResponse.append(inputLine);
                }
                in.close();
                log.error("Direct fetch failed. Code={}, Response={}", responseCode, errorResponse.toString());
                throw new RuntimeException("Direct fetch failed with code: " + responseCode);
            }
        } catch (Exception e) {
            log.error("Error fetching historical candle direct for key: " + normalizedKey, e);
            throw new RuntimeException("Error fetching historical candle direct", e);
        }
    }


    private com.am.marketdata.provider.upstox.model.HistoricalDataResponse mapToHistoricalDataResponse(
            GetHistoricalCandleResponse sdkResponse) {
        com.am.marketdata.provider.upstox.model.HistoricalDataResponse response = new com.am.marketdata.provider.upstox.model.HistoricalDataResponse();

        if (sdkResponse != null && sdkResponse.getStatus() != null) {
            response.setStatus(sdkResponse.getStatus().toString());
        }

        if (sdkResponse != null && sdkResponse.getData() != null && sdkResponse.getData().getCandles() != null) {
            com.am.marketdata.provider.upstox.model.HistoricalDataResponse.DataPayload dataPayload = new com.am.marketdata.provider.upstox.model.HistoricalDataResponse.DataPayload();
            dataPayload.setCandles(sdkResponse.getData().getCandles());
            response.setData(dataPayload);
        }

        return response;
    }

    private com.am.marketdata.provider.upstox.model.HistoricalDataResponse mapIntradayToHistoricalDataResponse(
            com.upstox.api.GetIntraDayCandleResponse sdkResponse) {
        com.am.marketdata.provider.upstox.model.HistoricalDataResponse response = new com.am.marketdata.provider.upstox.model.HistoricalDataResponse();

        if (sdkResponse != null && sdkResponse.getStatus() != null) {
            response.setStatus(sdkResponse.getStatus().toString());
        }

        if (sdkResponse != null && sdkResponse.getData() != null && sdkResponse.getData().getCandles() != null) {
            com.am.marketdata.provider.upstox.model.HistoricalDataResponse.DataPayload dataPayload = new com.am.marketdata.provider.upstox.model.HistoricalDataResponse.DataPayload();
            dataPayload.setCandles(sdkResponse.getData().getCandles());
            response.setData(dataPayload);
        }

        return response;
    }

}
