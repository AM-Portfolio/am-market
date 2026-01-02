package com.am.marketdata.provider.upstox;

import com.am.marketdata.provider.upstox.client.UpStockClient;
import com.am.marketdata.provider.upstox.config.UpstoxConfig;
import com.am.marketdata.provider.upstox.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for interacting with Upstox API
 */
@Slf4j
@Service
public class UpstoxApiService {

    @Value("${upstox.auth.api-key}")
    private String apiKey;

    @Value("${upstox.auth.secret-key}")
    private String apiSecret;

    @Value("${upstox.auth.redirect-uri}")
    private String redirectUri;

    private static final String REDIS_KEY_ACCESS_TOKEN = "market_data:upstox:access_token";

    private final UpStockClient upStockClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UpstoxConfig upstoxConfig;
    private String accessToken;

    @Autowired
    public UpstoxApiService(UpStockClient upStockClient, StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
            UpstoxConfig upstoxConfig) {
        this.upStockClient = upStockClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.upstoxConfig = upstoxConfig;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Upstox API service");
        try {
            // Try to load cached token from Redis
            String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
            if (cachedToken != null && !cachedToken.isEmpty()) {
                log.info("Found cached Access Token in Redis, applying to configuration");
                setAccessToken(cachedToken);
            } else {
                log.info("No cached Access Token found in Redis, checking configuration");
                if (upstoxConfig.getAccessToken() != null && !upstoxConfig.getAccessToken().isEmpty()) {
                    log.info("Found Access Token in configuration");
                    // We don't call setAccessToken to avoid overwriting config with itself or
                    // triggering side effects
                    // But setAccessToken updates local field and config.
                    // Since it's already in config, we just need to update local field.
                    this.accessToken = upstoxConfig.getAccessToken();
                } else {
                    log.warn("No Access Token found in Redis or Configuration");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load cached token from Redis (Redis might be down): {}", e.getMessage());
            // Fallback to config even on exception
            if (upstoxConfig.getAccessToken() != null && !upstoxConfig.getAccessToken().isEmpty()) {
                log.info("Found Access Token in configuration (fallback)");
                this.accessToken = upstoxConfig.getAccessToken();
            }
        }
    }

    public String getLoginUrl() {
        return "https://api.upstox.com/v2/login/authorization/dialog?response_type=code&client_id=" + apiKey
                + "&redirect_uri=" + redirectUri;
    }

    public Object generateSession(String code) {
        log.info("Generating Upstox session for code: {}", code);
        try {
            // Manual Token Exchange using Kong Unirest as used in UpStockClient
            kong.unirest.HttpResponse<String> response = kong.unirest.Unirest
                    .post("https://api-v2.upstox.com/login/authorization/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("code", code)
                    .field("client_id", apiKey)
                    .field("client_secret", apiSecret)
                    .field("redirect_uri", redirectUri)
                    .field("grant_type", "authorization_code")
                    .asString();

            if (response.getStatus() == 200) {
                String body = response.getBody();
                log.info("Successfully generated Upstox session");

                try {
                    // Parse response to extract access_token
                    JsonNode rootNode = objectMapper.readTree(body);
                    if (rootNode.has("access_token")) {
                        String newToken = rootNode.get("access_token").asText();
                        log.info("Extracted Access Token, saving to Redis");

                        // Save to cache (TTL 1 day or as appropriate)
                        redisTemplate.opsForValue().set(REDIS_KEY_ACCESS_TOKEN, newToken, 24, TimeUnit.HOURS);

                        // Update in-memory state
                        setAccessToken(newToken);
                    }
                } catch (Exception e) {
                    log.error("Error parsing token response: {}", e.getMessage());
                }

                return body;
            } else {
                throw new RuntimeException(
                        "Failed to generate token: " + response.getStatus() + " " + response.getBody());
            }

        } catch (Exception e) {
            log.error("Error generating Upstox session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Upstox session", e);
        }
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        if (upstoxConfig != null) {
            upstoxConfig.setAccessToken(accessToken);
        }
        // Also ensure UpStockClient knows about it if it doesn't pull from config
        // automatically
        // Assuming UpStockClient uses UpstoxConfig bean which we just updated.
    }

    public MarketQuoteResponse getLtp(List<String> symbols) {
        return upStockClient.getMarketQuotes(symbols);
    }

    public OHLCResponse getOhlc(List<String> symbols, String interval) {
        return upStockClient.getOHLCData(symbols, interval);
    }

    public HistoricalDataResponse getHistoricalCandleData(String symbol, String interval, String fromDate,
            String toDate) {
        return upStockClient.getHistoricalData(symbol, interval, fromDate, toDate);
    }

    public String getAccessToken() {
        return this.accessToken;
    }
}
