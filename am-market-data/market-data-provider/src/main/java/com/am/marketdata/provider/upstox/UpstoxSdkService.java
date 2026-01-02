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
                this.setAccessToken(cachedToken);
            } else {
                log.info("No cached Access Token found in Redis for SDK Service, checking configuration");
                if (upstoxConfig.getAccessToken() != null && !upstoxConfig.getAccessToken().isEmpty()) {
                    log.info("Found Access Token in configuration for SDK Service");
                    this.setAccessToken(upstoxConfig.getAccessToken());
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
        this.accessToken = accessToken;
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
        if (this.accessToken == null || this.accessToken.isEmpty()) {
            // Try to refresh from config one last time
            if (upstoxConfig.getAccessToken() != null) {
                this.accessToken = upstoxConfig.getAccessToken();
            }
            if (this.accessToken == null || this.accessToken.isEmpty()) {
                throw new IllegalStateException("Upstox Access token is not initialized");
            }
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

        // Join keys with comma
        String symbolList = String.join(",", instrumentKeys);

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
        if (this.accessToken == null || this.accessToken.isEmpty()) {
            // Try to refresh from config one last time
            if (upstoxConfig.getAccessToken() != null) {
                this.accessToken = upstoxConfig.getAccessToken();
            }
            if (this.accessToken == null || this.accessToken.isEmpty()) {
                throw new IllegalStateException("Upstox Access token is not initialized");
            }
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

        // Join keys with comma
        String symbolList = String.join(",", instrumentKeys);

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
     * Get historical candle data
     *
     * @param instrumentKey Instrument key (e.g. NSE_EQ|INE123...)
     * @param interval      Interval (e.g. 1minute, day, 30minute)
     * @param toDate        To date (YYYY-MM-DD or similar format required by API)
     * @param fromDate      From date (YYYY-MM-DD)
     * @return com.am.marketdata.provider.upstox.model.HistoricalDataResponse
     */
    public com.am.marketdata.provider.upstox.model.HistoricalDataResponse getHistoricalCandleData(String instrumentKey,
            String unit, Integer interval, String toDate, String fromDate) {
        if (this.accessToken == null || this.accessToken.isEmpty()) {
            if (upstoxConfig.getAccessToken() != null) {
                this.accessToken = upstoxConfig.getAccessToken();
            }
            if (this.accessToken == null || this.accessToken.isEmpty()) {
                throw new IllegalStateException("Upstox Access token is not initialized");
            }
        }

        try {
            // Initialize ApiClient
            ApiClient apiClient = new ApiClient();
            OAuth oAuth = (OAuth) apiClient.getAuthentication("OAUTH2");
            if (oAuth != null) {
                oAuth.setAccessToken(this.accessToken);
            } else {
                apiClient.setAccessToken(this.accessToken);
            }

            HistoryV3Api historyV3Api = new HistoryV3Api(apiClient);

            log.info("Fetching historical data for key: {}, unit: {}, interval: {}, from: {}, to: {}", instrumentKey,
                    unit, interval,
                    fromDate, toDate);

            // Call SDK
            GetHistoricalCandleResponse sdkResponse = historyV3Api.getHistoricalCandleData1(instrumentKey, unit,
                    interval,
                    toDate, fromDate);

            return mapToHistoricalDataResponse(sdkResponse);
        } catch (Exception e) {
            log.error("Error getting historical candle data from SDK", e);
            throw new RuntimeException("Error getting historical candle data", e);
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

}
