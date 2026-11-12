package com.am.marketdata.redis.cache;

import com.am.marketdata.common.model.analysis.CalendarHeatmapResponse;
import com.am.marketdata.common.model.analysis.SeasonalityResponse;
import com.am.marketdata.common.model.analysis.TechnicalAnalysisResponse;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.redis.config.RedisCacheCircuitBreaker;
import com.am.marketdata.redis.config.FeatureFlaggedStringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper redisObjectMapper;
    private final RedisCacheCircuitBreaker redisCircuitBreaker;

    @Value("${redis.cache.ttl.analysis:86400}") // Default 24h
    private long analysisTtlSeconds;

    @Value("${redis.cache.ttl.historical-performance:345600}")
    private long historicalPerformanceTtlSeconds;

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

    private String getIndexHeatmapKey(String symbol, String interval) {
        // Normalize interval so "1D" / "1d" share one key and forceRefresh overwrites reads.
        String tf = interval == null ? "" : interval.trim().toUpperCase();
        return String.format("%s:index:%s:%s", HEATMAP_PREFIX, symbol.toUpperCase(), tf);
    }

    /** Drop a cached index heatmap (e.g. after detecting a degenerate all-zero payload). */
    public void deleteIndexHeatmap(String symbol, String interval) {
        try {
            String key = getIndexHeatmapKey(symbol, interval);
            redisTemplate.delete(key);
            log.debug("deleteIndexHeatmap", "Deleted index heatmap cache for " + symbol + " " + interval);
        } catch (Exception e) {
            log.error("deleteIndexHeatmap",
                    "Error deleting index heatmap for " + symbol + ": " + e.getMessage());
        }
    }

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

    public void saveIndexHeatmap(String symbol, String interval, Map<String, Double> heatmap) {
        if (heatmap == null)
            return;
        try {
            String key = getIndexHeatmapKey(symbol, interval);
            String json = redisObjectMapper.writeValueAsString(heatmap);
            // 1D moves change every session — don't pin a bad/stale map for 30 days.
            String tf = interval == null ? "" : interval.trim().toUpperCase();
            long ttl = "1D".equals(tf) ? Math.min(analysisTtlSeconds, 3600L) : analysisTtlSeconds;
            redisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
            log.debug("saveIndexHeatmap", "Cached index heatmap for " + symbol + " ttlSec=" + ttl);
        } catch (Exception e) {
            log.error("saveIndexHeatmap",
                    "Error caching index heatmap for " + symbol + ": " + e.getMessage());
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

    public Map<String, Double> getIndexHeatmap(String symbol, String interval) {
        try {
            String key = getIndexHeatmapKey(symbol, interval);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return redisObjectMapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {
                        });
            }
        } catch (Exception e) {
            log.error("getIndexHeatmap", "Error retrieving index heatmap for " + symbol + ": " + e.getMessage());
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
    // --- HISTORICAL PERFORMANCE ---

    private static final String HISTORY_PREFIX = "analysis:history";

    private String getHistoryKey(String symbol, int years, boolean detailed) {
        return String.format("%s:%s:%d:%b", HISTORY_PREFIX, symbol.toUpperCase(), years, detailed);
    }

    public void saveHistoricalPerformance(
            com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse response, int years,
            boolean detailed) {
        if (response == null)
            return;
        if (!isRedisFeatureEnabled()) {
            log.debug("saveHistoricalPerformance", "Redis feature is disabled; skipping cache write");
            return;
        }
        if (!redisCircuitBreaker.allowRequest()) {
            log.debug("saveHistoricalPerformance", "Redis circuit is open; skipping analysis cache write");
            return;
        }
        try {
            String key = getHistoryKey(response.getSymbol(), years, detailed);
            String json = redisObjectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, historicalPerformanceTtlSeconds, TimeUnit.SECONDS);
            redisCircuitBreaker.recordSuccess();
            log.debug("saveHistoricalPerformance", "Cached historical performance for " + response.getSymbol());
        } catch (Exception e) {
            redisCircuitBreaker.recordFailure("analysis-write", e);
            log.error("saveHistoricalPerformance",
                    "Error caching historical performance for " + response.getSymbol() + ": " + e.getMessage());
        }
    }

    public com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse getHistoricalPerformance(String symbol,
            int years, boolean detailed) {
        if (!isRedisFeatureEnabled()) {
            log.debug("getHistoricalPerformance", "Redis feature is disabled; treating analysis cache as unavailable");
            return null;
        }
        if (!redisCircuitBreaker.allowRequest()) {
            log.debug("getHistoricalPerformance", "Redis circuit is open; treating analysis cache as unavailable");
            return null;
        }
        try {
            String key = getHistoryKey(symbol, years, detailed);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                redisCircuitBreaker.recordSuccess();
                return redisObjectMapper.readValue(json,
                        com.am.marketdata.common.model.analysis.HistoricalPerformanceResponse.class);
            }
            redisCircuitBreaker.recordSuccess();
        } catch (Exception e) {
            redisCircuitBreaker.recordFailure("analysis-read", e);
            log.error("getHistoricalPerformance",
                    "Error retrieving historical performance for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    private boolean isRedisFeatureEnabled() {
        return !(redisTemplate instanceof FeatureFlaggedStringRedisTemplate featureFlaggedTemplate)
                || featureFlaggedTemplate.isRedisEnabled();
    }
    // --- INDICES HISTORICAL PERFORMANCE (AGGREGATE) ---

    private static final String INDICES_HISTORY_PREFIX = "analysis:history:indices";

    private String getIndicesHistoryKey(int years) {
        return String.format("%s:%d", INDICES_HISTORY_PREFIX, years);
    }

    public void saveIndicesHistoricalPerformance(
            com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse response, int years) {
        if (response == null)
            return;
        try {
            String key = getIndicesHistoryKey(years);
            String json = redisObjectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, analysisTtlSeconds, TimeUnit.SECONDS);
            log.debug("saveIndicesHistoricalPerformance",
                    "Cached indices historical performance for " + years + " years");
        } catch (Exception e) {
            log.error("saveIndicesHistoricalPerformance",
                    "Error caching indices historical performance: " + e.getMessage());
        }
    }

    public com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse getIndicesHistoricalPerformance(
            int years) {
        try {
            String key = getIndicesHistoryKey(years);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return redisObjectMapper.readValue(json,
                        com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse.class);
            }
        } catch (Exception e) {
            log.error("getIndicesHistoricalPerformance",
                    "Error retrieving indices historical performance: " + e.getMessage());
        }
        return null;
    }

    // --- CONSTITUENTS HISTORICAL PERFORMANCE (PER INDEX) ---

    private static final String CONSTITUENTS_HISTORY_PREFIX = "analysis:history:constituents";

    private String getConstituentsHistoryKey(String indexSymbol, int years) {
        String norm = indexSymbol == null ? "" : indexSymbol.trim().toUpperCase().replace(' ', '_');
        return String.format("%s:%s:%d", CONSTITUENTS_HISTORY_PREFIX, norm, years);
    }

    public void saveConstituentsHistoricalPerformance(
            com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse response,
            String indexSymbol, int years) {
        if (response == null)
            return;
        try {
            String key = getConstituentsHistoryKey(indexSymbol, years);
            String json = redisObjectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, analysisTtlSeconds, TimeUnit.SECONDS);
            log.debug("saveConstituentsHistoricalPerformance",
                    "Cached constituents historical performance for " + indexSymbol + " / " + years + "y");
        } catch (Exception e) {
            log.error("saveConstituentsHistoricalPerformance",
                    "Error caching constituents historical performance: " + e.getMessage());
        }
    }

    public com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse getConstituentsHistoricalPerformance(
            String indexSymbol, int years) {
        try {
            String key = getConstituentsHistoryKey(indexSymbol, years);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return redisObjectMapper.readValue(json,
                        com.am.marketdata.common.model.analysis.IndicesHistoricalPerformanceResponse.class);
            }
        } catch (Exception e) {
            log.error("getConstituentsHistoricalPerformance",
                    "Error retrieving constituents historical performance: " + e.getMessage());
        }
        return null;
    }

    // --- MOVERS CACHE ---

    private static final String MOVERS_PREFIX = "analysis:movers";

    /**
     * Builds standard Redis cache key for top movers:
     * analysis:movers:{INDEX_SYMBOL}:{TIMEFRAME}:{LIMIT}:{TYPE}:{EXPAND_INDICES}
     */
    private String getMoversKey(String indexSymbol, String timeFrame, int limit, String type, boolean expandIndices) {
        String normIndex = normalizeIndexSymbol(indexSymbol);
        String normTf = (timeFrame == null || timeFrame.isBlank()) ? "1D" : timeFrame.trim().toUpperCase(java.util.Locale.ROOT);
        String normType = (type == null || type.isBlank()) ? "ALL" : type.trim().toUpperCase(java.util.Locale.ROOT);
        return String.format("%s:%s:%s:%d:%s:%b", MOVERS_PREFIX, normIndex, normTf, limit, normType, expandIndices);
    }

    /**
     * Normalizes index symbol to match the exact `_id` stored in MongoDB `stock_indices_market_data`.
     * Strips exchange prefixes (e.g., 'NSE:', 'BSE:'), trims leading/trailing whitespace, and uppercases.
     */
    public String normalizeIndexSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "NIFTY 50";
        }
        String clean = symbol.trim().toUpperCase(java.util.Locale.ROOT);
        while (clean.contains(":")) {
            clean = clean.substring(clean.indexOf(":") + 1).trim();
        }
        return clean;
    }

    /**
     * Computes dynamic TTL based on Indian Market trading hours (Asia/Kolkata):
     * - Regular Market Hours (09:15 - 15:30 IST on Weekdays): Short 30s TTL for real-time freshness.
     * - Off-Market Hours / Weekends: Cache is capped to next market opening (09:15 IST) up to a max of 3600s (1h).
     * This prevents off-market static data from persisting into live market trading sessions.
     */
    public long calculateMoversTtlSeconds() {
        java.time.ZoneId istZone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(istZone);
        java.time.DayOfWeek day = now.getDayOfWeek();
        java.time.LocalTime time = now.toLocalTime();

        boolean isWeekday = (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY);
        java.time.LocalTime marketOpen = java.time.LocalTime.of(9, 15);
        java.time.LocalTime marketClose = java.time.LocalTime.of(15, 30);

        boolean isMarketOpen = isWeekday && !time.isBefore(marketOpen) && !time.isAfter(marketClose);

        if (isMarketOpen) {
            return 30L;
        }

        // Off-market: calculate remaining seconds until next weekday 09:15 IST market open
        java.time.ZonedDateTime nextOpen = now.with(marketOpen).withSecond(0).withNano(0);
        if (time.isAfter(marketClose) || !isWeekday) {
            nextOpen = nextOpen.plusDays(1);
            while (nextOpen.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || nextOpen.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                nextOpen = nextOpen.plusDays(1);
            }
        }

        long secondsUntilOpen = java.time.Duration.between(now, nextOpen).getSeconds();
        if (secondsUntilOpen <= 0) {
            return 30L;
        }
        // Cap to 1 hour max, or until market open if closer
        return Math.min(3600L, secondsUntilOpen);
    }

    /**
     * Saves JSON payload of calculated movers into Redis with circuit-breaker protection and dynamic TTL.
     */
    public void saveMoversRawJson(String indexSymbol, String timeFrame, int limit, String type, boolean expandIndices, String json) {
        if (json == null || json.isBlank()) return;
        if (!isRedisFeatureEnabled() || !redisCircuitBreaker.allowRequest()) return;
        try {
            String key = getMoversKey(indexSymbol, timeFrame, limit, type, expandIndices);
            long ttl = calculateMoversTtlSeconds();
            redisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
            redisCircuitBreaker.recordSuccess();
            log.debug("saveMoversRawJson", "Cached movers for " + key + " with ttlSec=" + ttl);
        } catch (Exception e) {
            redisCircuitBreaker.recordFailure("movers-write", e);
            log.error("saveMoversRawJson", "Error caching movers: " + e.getMessage());
        }
    }

    /**
     * Retrieves precomputed movers JSON from Redis if available and circuit is healthy.
     */
    public String getMoversRawJson(String indexSymbol, String timeFrame, int limit, String type, boolean expandIndices) {
        if (!isRedisFeatureEnabled() || !redisCircuitBreaker.allowRequest()) return null;
        try {
            String key = getMoversKey(indexSymbol, timeFrame, limit, type, expandIndices);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                redisCircuitBreaker.recordSuccess();
                return json;
            }
        } catch (Exception e) {
            redisCircuitBreaker.recordFailure("movers-read", e);
            log.error("getMoversRawJson", "Error retrieving movers cache: " + e.getMessage());
        }
        return null;
    }

    public com.fasterxml.jackson.databind.ObjectMapper getRedisObjectMapper() {
        return this.redisObjectMapper;
    }
}
