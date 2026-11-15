package com.am.marketdata.api.service.global;

import com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.service.global.GlobalMarketScheduleService;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.am.common.investment.service.StockIndicesMarketDataService;
import java.util.concurrent.CompletableFuture;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Service responsible for fetching the latest prices of global market indices.
 *
 * <p><b>Data Source Priority (per symbol):</b>
 * <ol>
 *   <li><b>Redis Cache ({@code market:global-latest:<INSTRUMENT_KEY>})</b> —
 *       Populated by {@code StreamerManager.handleGlobalIndexTick()} on every WebSocket tick.
 *       This is the primary and fastest path.</li>
 *   <li><b>REST Fallback (Upstox Quote API)</b> —
 *       Triggered when the Redis cache is empty (cold start after server restart,
 *       or first request of a new global market session). Protected by a Redis
 *       distributed lock to prevent simultaneous API spam from concurrent requests.</li>
 * </ol>
 *
 * <p><b>Response Contract:</b>
 * Returns {@link StockIndicesMarketData} — the same DTO used for Indian indices.
 * Indian-specific fields ({@code constituents}, {@code marketBreadth}, etc.) are
 * intentionally left null. The {@code metadata.segment} field is set to "GLOBAL",
 * and {@code metadata.suspended} may be set to true if the circuit breaker is active.
 * These new fields are ignored by existing Indian-market microservices because
 * {@code IndexMetadata} is annotated with {@code @JsonIgnoreProperties(ignoreUnknown = true)}.
 *
 * <p><b>Market Hours Gate:</b>
 * This service does NOT use the Indian NSE 9:15–15:45 IST market hours check.
 * The REST fallback is triggered based on cache staleness alone (5-minute threshold),
 * allowing the service to function correctly during US/European market hours.
 *
 * <p><b>IMPORTANT:</b> This service must NOT use {@code StockIndicesService} internally.
 * It is a completely independent code path to prevent any logic from the Indian
 * market pipeline from leaking into the global index pipeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalIndexService {

    /**
     * Redis key prefix for global index latest prices.
     * Format: market:global-latest:<INSTRUMENT_KEY>
     * Matches the prefix written by StreamerManager.handleGlobalIndexTick().
     */
    private static final String GLOBAL_LATEST_PREFIX = "market:global-latest:";

    /**
     * Redis key prefix for global index tick timestamps.
     * Format: market:global-timestamp:<INSTRUMENT_KEY>
     * Used to check cache staleness for the REST fallback decision.
     */
    private static final String GLOBAL_TIMESTAMP_PREFIX = "market:global-timestamp:";

    /**
     * Redis key prefix for the circuit breaker suspension flag.
     * Format: market:global-suspended:<INSTRUMENT_KEY>
     * Set to "true" when no tick received for 30+ minutes during expected market hours.
     */
    private static final String GLOBAL_SUSPENDED_PREFIX = "market:global-suspended:";

    /**
     * Redis key prefix for the distributed lock protecting the REST fallback.
     * Ensures only one thread/pod fetches from Upstox Quote API at a time
     * for each symbol on cold-start or cache miss.
     */
    private static final String GLOBAL_LOCK_PREFIX = "market:lock:global:";

    /**
     * TTL for the distributed lock in seconds.
     * Short enough to recover quickly if a lock holder crashes mid-fetch.
     */
    private static final long LOCK_TTL_SECONDS = 10L;
    private static final long MONGO_SAVE_COOLDOWN_MS = 5 * 60 * 1000L; // 5 minutes
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);
    private static final DefaultRedisScript<Long> WRITE_NEWER_QUOTE_SCRIPT = new DefaultRedisScript<>(
            "local old=redis.call('get', KEYS[2]); " +
                    "if old and tonumber(old) and tonumber(old) >= tonumber(ARGV[1]) then return 0 end; " +
                    "redis.call('set', KEYS[1], ARGV[2]); redis.call('set', KEYS[2], ARGV[1]); return 1",
            Long.class);

    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastMongoSaveTimeMap = new java.util.concurrent.ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final GlobalIndexConfigRepository globalIndexConfigRepository;
    private final ObjectMapper objectMapper;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final GlobalMarketScheduleService globalMarketScheduleService;

    /**
     * Used for the REST fallback when Redis cache is empty or stale.
     * Calls Upstox Quote API via the same provider layer used for Indian indices.
     * MUST NOT be used for every request — only on lock-protected cache miss.
     */
    private final MarketDataFetchService marketDataFetchService;

    @PostConstruct
    public void init() {
        if (objectMapper != null) {
            objectMapper.findAndRegisterModules();
        }
    }

    /**
     * Fetches the latest prices for a list of global index symbols.
     *
     * <p>For each symbol, the method attempts to read from Redis first.
     * If the cached price is stale (older than {@link #STALE_THRESHOLD_MS}),
     * it acquires a distributed lock and triggers a one-shot REST fallback
     * to the Upstox Quote API.
     *
     * <p>Symbols that cannot be found in MongoDB config or Redis are returned
     * with their name populated and all price fields as null, with
     * {@code metadata.suspended = true} to signal a data unavailability condition.
     *
     * @param symbols the list of global index symbols to fetch (e.g., ["DJI", "SPX"])
     * @return list of {@link StockIndicesMarketData} in the same order as input symbols
     */
    public List<StockIndicesMarketData> getLatestGlobalIndices(List<String> symbols) {
        List<StockIndicesMarketData> results = new ArrayList<>();

        for (String symbol : symbols) {
            try {
                StockIndicesMarketData data = fetchGlobalIndexData(symbol);
                results.add(data);
            } catch (Exception e) {
                log.error("[GlobalIndexService] Unexpected error fetching data for symbol={}: {}", symbol, e.getMessage(), e);
                // Return a partial response rather than failing the entire batch
                results.add(buildFallbackResponse(symbol, true));
            }
        }

        return results;
    }

    /**
     * Fetches data for a single global index symbol.
     *
     * <p>Flow:
     * <ol>
     *   <li>Look up the MongoDB config to get the Upstox instrument key.</li>
     *   <li>Check Redis for a cached price.</li>
     *   <li>If stale, try a REST fallback (with distributed lock protection).</li>
     *   <li>Check the circuit breaker suspension flag.</li>
     *   <li>Map the result to {@link StockIndicesMarketData}.</li>
     * </ol>
     *
     * @param symbol the global index symbol
     * @return the market data response DTO
     */
    private StockIndicesMarketData fetchGlobalIndexData(String symbol) {
        // Step 1: Look up schedule (the authoritative source) and MongoDB config
        GlobalMarketScheduleService.MarketEntry schedule = globalMarketScheduleService.getMarkets().stream()
                .filter(entry -> symbol.equals(entry.symbol()))
                .findFirst()
                .orElse(null);

        Optional<GlobalIndexConfigDocument> configOpt = globalIndexConfigRepository.findBySymbol(symbol);
        GlobalIndexConfigDocument config = configOpt.orElse(null);

        // Authoritative Instrument Key: schedule.instrumentKey() takes precedence whenever present
        String instrumentKey = schedule != null && schedule.hasInstrument()
                ? schedule.instrumentKey()
                : (config != null ? config.getInstrumentKey() : null);

        if (instrumentKey == null || instrumentKey.isBlank()) {
            log.warn("[GlobalIndexService] No verified provider instrument for symbol={}; returning fallback.", symbol);
            return buildFallbackResponse(symbol, true);
        }

        // If config is missing in MongoDB, build an in-memory document from the schedule
        if (config == null && schedule != null) {
            config = GlobalIndexConfigDocument.builder()
                    .symbol(symbol)
                    .name(schedule.name())
                    .instrumentKey(instrumentKey)
                    .exchange(schedule.exchange())
                    .timezone(schedule.timezone())
                    .marketOpenTime(schedule.marketOpen())
                    .marketCloseTime(schedule.marketClose())
                    .influxAggregateOffset(schedule.influxAggregateOffset())
                    .build();
        }

        // Step 2: Try Redis cache first (fastest path — populated by WebSocket ticks)
        String cachedJson = redisTemplate.opsForValue().get(GLOBAL_LATEST_PREFIX + instrumentKey);
        boolean isStale = !isUsableQuote(cachedJson, schedule, Instant.now());

        // Step 3: If stale or missing, acquire distributed lock and trigger REST fallback
        if ((cachedJson == null || isStale)) {
            String lockKey = GLOBAL_LOCK_PREFIX + instrumentKey;
            String lockToken = UUID.randomUUID().toString();
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockToken, LOCK_TTL_SECONDS, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(lockAcquired)) {
                log.info("[GlobalIndexService] Cache miss/stale for instrumentKey={}. Fetching from Upstox REST.", instrumentKey);
                try {
                    Map<String, OHLCQuote> ohlcResult = marketDataFetchService.getOHLC(
                            Set.of(instrumentKey), true, TimeFrame.DAY, true);

                    if (ohlcResult != null && ohlcResult.containsKey(instrumentKey)) {
                        OHLCQuote freshQuote = ohlcResult.get(instrumentKey);
                        if (freshQuote != null && freshQuote.getLastPrice() > 0.0) {
                            String freshJson = objectMapper.writeValueAsString(freshQuote);
                            cachedJson = freshJson;
                            long quoteTime = freshQuote.getQuoteTimestamp() != null
                                    ? freshQuote.getQuoteTimestamp().toEpochMilli()
                                    : System.currentTimeMillis();
                            try {
                                redisTemplate.execute(WRITE_NEWER_QUOTE_SCRIPT,
                                        List.of(GLOBAL_LATEST_PREFIX + instrumentKey, GLOBAL_TIMESTAMP_PREFIX + instrumentKey),
                                        String.valueOf(quoteTime), freshJson);
                            } catch (Exception ex) {
                                log.warn("[GlobalIndexService] Failed to write newer quote to Redis for key={}: {}", instrumentKey, ex.getMessage());
                            }
                            log.info("[GlobalIndexService] REST fallback succeeded for instrumentKey={}. lastPrice={}",
                                    instrumentKey, freshQuote.getLastPrice());
                        } else {
                            log.warn("[GlobalIndexService] Upstox REST returned no valid data (price <= 0.0) for instrumentKey={}.", instrumentKey);
                        }
                    }
                } catch (Exception e) {
                    log.error("[GlobalIndexService] REST fallback failed for instrumentKey={}: {}", instrumentKey, e.getMessage(), e);
                } finally {
                    redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockToken);
                }
            } else {
                log.debug("[GlobalIndexService] Refresh lock held for instrumentKey={}; using existing cache.", instrumentKey);
            }
        }

        // Step 4: Check circuit breaker
        boolean isSuspended = "true".equalsIgnoreCase(
                redisTemplate.opsForValue().get(GLOBAL_SUSPENDED_PREFIX + instrumentKey));

        // Step 5: Map Redis cache to StockIndicesMarketData DTO if available
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                OHLCQuote q = objectMapper.readValue(cachedJson, OHLCQuote.class);
                if (q != null && q.getLastPrice() > 0.0) {
                    StockIndicesMarketData data = buildResponse(symbol, config, cachedJson, isSuspended);
                    saveToMongoAsync(data, instrumentKey);
                    return data;
                }
            } catch (Exception e) {
                log.warn("[GlobalIndexService] Error parsing cached JSON for {}: {}", symbol, e.getMessage());
            }
        }

        // Step 6: Database Fallback — retrieve last-known closing quote from MongoDB
        try {
            StockIndicesMarketData lastKnown = findLastKnownFromMongo(symbol, instrumentKey);
            if (lastKnown != null && lastKnown.getMetadata() != null && lastKnown.getMetadata().getLast() != null && lastKnown.getMetadata().getLast() > 0) {
                log.info("[GlobalIndexService] Serving last confirmed database values for symbol={} (key={})", symbol, instrumentKey);
                lastKnown.setIndexSymbol(symbol);
                lastKnown.getMetadata().setSuspended(isSuspended);
                return lastKnown;
            }
        } catch (Exception e) {
            log.error("[GlobalIndexService] Failed to load last-known data from MongoDB for symbol={} (key={}): {}", symbol, instrumentKey, e.getMessage());
        }

        log.warn("[GlobalIndexService] No quote available for symbol={}; returning suspended fallback.", symbol);
        return buildFallbackResponse(symbol, true);
    }

    private StockIndicesMarketData findLastKnownFromMongo(String symbol, String instrumentKey) {
        StockIndicesMarketData doc = stockIndicesMarketDataService.findByIndexSymbol(instrumentKey);
        if (doc != null) return doc;

        doc = stockIndicesMarketDataService.findByIndexSymbol(symbol);
        if (doc != null) return doc;

        String unCaret = instrumentKey.replace("^", "");
        if (!unCaret.equals(instrumentKey)) {
            doc = stockIndicesMarketDataService.findByIndexSymbol(unCaret);
            if (doc != null) return doc;
        }
        return null;
    }

    /**
     * Persists the global index market data asynchronously back to MongoDB.
     * Uses a cooldown to debounce writes.
     */
    private void saveToMongoAsync(StockIndicesMarketData data, String instrumentKey) {
        String symbol = data.getIndexSymbol();
        long now = System.currentTimeMillis();
        Long lastSave = lastMongoSaveTimeMap.get(symbol);
        if (lastSave != null && (now - lastSave) < MONGO_SAVE_COOLDOWN_MS) {
            return; // Cooldown active, skip saving
        }
        lastMongoSaveTimeMap.put(symbol, now);

        CompletableFuture.runAsync(() -> {
            try {
                // Save document with indexSymbol set to the prefixed instrumentKey (e.g. GLOBAL_INDEX|DJI)
                StockIndicesMarketData dbData = StockIndicesMarketData.builder()
                        .indexSymbol(instrumentKey)
                        .metadata(data.getMetadata())
                        .audit(data.getAudit())
                        .build();

                if (dbData.getAudit() == null) {
                    com.am.common.investment.model.stockindice.AuditData audit = new com.am.common.investment.model.stockindice.AuditData();
                    audit.setCreatedAt(java.time.LocalDateTime.now());
                    audit.setUpdatedAt(java.time.LocalDateTime.now());
                    dbData.setAudit(audit);
                } else {
                    dbData.getAudit().setUpdatedAt(java.time.LocalDateTime.now());
                }
                stockIndicesMarketDataService.save(dbData);
                log.info("[GlobalIndexService] Successfully persisted global index {} to MongoDB", instrumentKey);
            } catch (Exception e) {
                log.error("[GlobalIndexService] Failed to save global index data to MongoDB for symbol={}", symbol, e);
            }
        });
    }

    /**
     * Checks whether the cached timestamp indicates a stale cache entry.
     *
     * @param cachedTimestamp epoch millisecond string from Redis, or null if absent
     * @return true if the cache is stale or missing
     */
    private boolean isUsableQuote(String json, GlobalMarketScheduleService.MarketEntry schedule, Instant now) {
        if (json == null || json.isBlank()) return false;
        try {
            return GlobalIndexQuoteValidator.isUsable(objectMapper.readValue(json, OHLCQuote.class), schedule,
                    globalMarketScheduleService, now);
        } catch (Exception e) {
            log.warn("[GlobalIndexService] Rejecting unreadable global quote for symbol={}", schedule.symbol());
            return false;
        }
    }

    private boolean isProviderDelayed(String json, GlobalMarketScheduleService.MarketEntry schedule, Instant now) {
        try {
            OHLCQuote quote = objectMapper.readValue(json, OHLCQuote.class);
            return GlobalIndexQuoteValidator.isProviderDelayed(quote, schedule, globalMarketScheduleService, now);
        } catch (Exception ignored) {
            return true;
        }
    }

    /**
     * Builds a {@link StockIndicesMarketData} response from a cached JSON string.
     *
     * <p>This method maps the {@link OHLCQuote} from Redis to the shared DTO.
     * Indian-specific fields ({@code constituents}, {@code data} list, {@code audit})
     * are left null — Jackson's {@code @JsonInclude(NON_NULL)} ensures they are
     * omitted from the JSON response, preserving the contract for existing consumers.
     *
     * @param symbol      the human-readable index symbol
     * @param config      the MongoDB config document for display metadata
     * @param cachedJson  the JSON string of the {@link OHLCQuote} from Redis
     * @param isSuspended whether the circuit breaker is active for this index
     * @return the populated market data DTO
     */
    private StockIndicesMarketData buildResponse(String symbol, GlobalIndexConfigDocument config,
                                                  String cachedJson, boolean isSuspended) {
        try {
            OHLCQuote quote = objectMapper.readValue(cachedJson, OHLCQuote.class);

            double lastPrice = quote.getLastPrice();
            double open = quote.getOhlc() != null ? quote.getOhlc().getOpen() : 0.0;
            double high = quote.getOhlc() != null ? quote.getOhlc().getHigh() : 0.0;
            double low = quote.getOhlc() != null ? quote.getOhlc().getLow() : 0.0;
            double prevClose = quote.getPreviousClose();
            double changePercent = prevClose > 0 ? ((lastPrice - prevClose) / prevClose) * 100 : 0.0;

            // Build IndexMetadata — using the nested metadata object rather than
            // adding flat fields to StockIndicesMarketData, preserving the DTO contract.
            // The new `segment` and `suspended` fields are optional and null-safe for
            // all existing consumers.
            IndexMetadata metadata = new IndexMetadata();
            metadata.setIndexName(config != null && config.getName() != null ? config.getName() : symbol);
            metadata.setOpen(open);
            metadata.setHigh(high);
            metadata.setLow(low);
            metadata.setPreviousClose(prevClose);
            metadata.setLast(lastPrice);
            metadata.setChange(lastPrice - prevClose);
            metadata.setPercChange(changePercent);
            metadata.setSegment("GLOBAL"); // identifies this as a foreign market index
            metadata.setSuspended(isSuspended);

            return StockIndicesMarketData.builder()
                    .indexSymbol(symbol)
                    .metadata(metadata)
                    // data (constituent stocks) is intentionally null for global indices
                    // — Indian-market consumers handle null data gracefully
                    .build();

        } catch (Exception e) {
            log.error("[GlobalIndexService] Failed to parse cached data for symbol={}: {}", symbol, e.getMessage(), e);
            return buildFallbackResponse(symbol, true);
        }
    }

    /**
     * Builds a minimal fallback {@link StockIndicesMarketData} when no price data
     * is available (e.g., cold start, connectivity issue, or unknown symbol).
     *
     * <p>The {@code suspended} flag is set to {@code true} to signal to the frontend
     * that the displayed data is unavailable rather than zero.
     *
     * @param symbol      the index symbol
     * @param isSuspended whether to mark this as suspended
     * @return a minimal DTO with suspended metadata
     */
    private StockIndicesMarketData buildFallbackResponse(String symbol, boolean isSuspended) {
        IndexMetadata metadata = new IndexMetadata();
        metadata.setSegment("GLOBAL");
        if (isSuspended) {
            metadata.setSuspended(true);
        }
        return StockIndicesMarketData.builder()
                .indexSymbol(symbol)
                .metadata(metadata)
                .build();
    }
}
