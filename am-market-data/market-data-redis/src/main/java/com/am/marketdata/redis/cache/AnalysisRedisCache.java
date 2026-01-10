package com.am.marketdata.redis.cache;

import com.am.marketdata.common.model.analysis.CalendarHeatmapResponse;
import com.am.marketdata.common.model.analysis.SeasonalityResponse;
import com.am.marketdata.common.model.analysis.TechnicalAnalysisResponse;
import com.am.marketdata.common.model.TimeFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class AnalysisRedisCache {

    private final AppLogger log = AppLogger.getLogger();
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    @Value("${redis.cache.ttl.analysis:86400}") // Default 24h
    private long analysisTtlSeconds;

    private static final String SEASONALITY_PREFIX = "analysis:seasonality";
    private static final String HEATMAP_PREFIX = "analysis:heatmap";
    private static final String TECHNICAL_PREFIX = "analysis:technical";

    // --- KEY GENERATORS ---

    private String getSeasonalityKey(String symbol, String interval) {
        return String.format("%s:%s:%s", SEASONALITY_PREFIX, symbol.toUpperCase(), interval);
    }

    private String getHeatmapKey(String symbol, int year) {
        return String.format("%s:%s:%d", HEATMAP_PREFIX, symbol.toUpperCase(), year);
    }

    private String getTechnicalKey(String symbol, String interval) {
        return String.format("%s:%s:%s", TECHNICAL_PREFIX, symbol.toUpperCase(), interval);
    }

    // --- SEASONALITY ---

    public void saveSeasonality(SeasonalityResponse response, TimeFrame interval) {
        if (response == null)
            return;
        try {
            String key = getSeasonalityKey(response.getSymbol(), interval.name());
            String json = redisObjectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, analysisTtlSeconds, TimeUnit.SECONDS);
            log.debug("saveSeasonality", "Cached seasonality for " + response.getSymbol());
        } catch (Exception e) {
            log.error("saveSeasonality",
                    "Error caching seasonality for " + response.getSymbol() + ": " + e.getMessage());
        }
    }

    public SeasonalityResponse getSeasonality(String symbol, TimeFrame interval) {
        try {
            String key = getSeasonalityKey(symbol, interval.name());
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return redisObjectMapper.readValue(json, SeasonalityResponse.class);
            }
        } catch (Exception e) {
            log.error("getSeasonality", "Error retrieving seasonality for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    // --- HEATMAP ---

    public void saveHeatmap(CalendarHeatmapResponse response, int year) {
        if (response == null)
            return;
        try {
            String key = getHeatmapKey(response.getSymbol(), year);
            String json = redisObjectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, analysisTtlSeconds, TimeUnit.SECONDS);
            log.debug("saveHeatmap", "Cached heatmap for " + response.getSymbol());
        } catch (Exception e) {
            log.error("saveHeatmap", "Error caching heatmap for " + response.getSymbol() + ": " + e.getMessage());
        }
    }

    public CalendarHeatmapResponse getHeatmap(String symbol, int year) {
        try {
            String key = getHeatmapKey(symbol, year);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return redisObjectMapper.readValue(json, CalendarHeatmapResponse.class);
            }
        } catch (Exception e) {
            log.error("getHeatmap", "Error retrieving heatmap for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    // --- TECHNICAL ---

    public void saveTechnical(TechnicalAnalysisResponse response, TimeFrame interval) {
        if (response == null)
            return;
        try {
            String key = getTechnicalKey(response.getSymbol(), interval.name());
            String json = redisObjectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, analysisTtlSeconds, TimeUnit.SECONDS);
            log.debug("saveTechnical", "Cached technical for " + response.getSymbol());
        } catch (Exception e) {
            log.error("saveTechnical", "Error caching technical for " + response.getSymbol() + ": " + e.getMessage());
        }
    }

    public TechnicalAnalysisResponse getTechnical(String symbol, TimeFrame interval) {
        try {
            String key = getTechnicalKey(symbol, interval.name());
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return redisObjectMapper.readValue(json, TechnicalAnalysisResponse.class);
            }
        } catch (Exception e) {
            log.error("getTechnical", "Error retrieving technical for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    // --- BATCH HELPERS ---

    public void saveSeasonalityBatch(List<SeasonalityResponse> responses, TimeFrame interval) {
        if (responses == null || responses.isEmpty())
            return;
        try {
            Map<String, String> keyValues = new HashMap<>();
            for (SeasonalityResponse res : responses) {
                String key = getSeasonalityKey(res.getSymbol(), interval.name());
                String json = redisObjectMapper.writeValueAsString(res);
                keyValues.put(key, json);
            }
            redisTemplate.opsForValue().multiSet(keyValues);

            // TTL needs to be set individually unfortunately
            // But we can pipeline it
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (String key : keyValues.keySet()) {
                    connection.expire(redisTemplate.getStringSerializer().serialize(key), analysisTtlSeconds);
                }
                return null;
            });
            log.debug("saveSeasonalityBatch", "Cached batch seasonality for " + responses.size() + " items");

        } catch (Exception e) {
            log.error("saveSeasonalityBatch", "Error batch caching seasonality: " + e.getMessage());
        }
    }
}
