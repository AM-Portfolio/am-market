package com.am.marketdata.service;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;
import com.am.marketdata.service.calendar.MarketCalendarService;
import com.am.common.investment.model.equity.EquityPrice;

import lombok.RequiredArgsConstructor;
import com.am.marketdata.common.log.AppLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service to provide "Smart" stock data by combining Cache, Database, and
 * Historical data.
 * Adheres to the strict rule: NEVER call provider for read operations.
 */
@Service
@RequiredArgsConstructor
public class SmartStockService {

    private static final DefaultRedisScript<Long> RELEASE_REFRESH_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final AppLogger log = AppLogger.getLogger(SmartStockService.class);
    private final MarketDataService marketDataService;
    private final MarketDataCacheService marketDataCacheService;

    // Optional in tests; production requires Redis coordination before provider recovery.
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private com.am.marketdata.service.repo.PreviousCloseRepository previousCloseRepository;

    @Autowired(required = false)
    private MarketCalendarService marketCalendarService;

    @Value("${market.data.movers.refresh-lock.ttl-seconds:30}")
    private long refreshLockTtlSeconds;

    @Value("${market.data.movers.refresh-lock.wait-ms:500}")
    private long refreshLockWaitMs;

    /**
     * Get the latest quotes for a list of symbols with TimeFrame support.
     * If TimeFrame is provided and not DAY, it fetches historical data to calculate
     * change/pChange
     * relative to that timeframe (e.g. 1 Year ago).
     */
    public Map<String, OHLCQuote> getSmartQuotes(List<String> symbols, TimeFrame timeFrame) {
        // 1. Get Base Quotes (Live/Latest)
        Map<String, OHLCQuote> currentQuotes = getSmartQuotes(symbols);

        if (timeFrame == null || timeFrame == TimeFrame.DAY) {
            backfillPreviousCloseForDay(currentQuotes);
            return currentQuotes; // Default behavior
        }

        log.info("getSmartQuotes", "Calculating quotes for TimeFrame: " + timeFrame);

        // 2. Fetch Historical Data for Previous Reference Point
        ZoneId exchangeZone = ZoneId.of("Asia/Kolkata");
        LocalDate to = LocalDate.now(exchangeZone);
        LocalDate from = getStartDateForTimeFrame(to, timeFrame);

        try {
            // We need data around 'from' date to get the close price at that time
            // We ask for a small buffer around 'from' date to ensure we get a point
            Date fromDate = Date.from(from.minusDays(10).atStartOfDay(exchangeZone).toInstant());
            // We only need up to 'from' date essentially, but let's ask for a range to be
            // safe
            Date toDate = Date.from(from.plusDays(5).atStartOfDay(exchangeZone).toInstant());

            // A timeframe base is historical data, so a cache/database miss must
            // not fan out into individual Upstox candle requests.
            Map<String, com.am.common.investment.model.historical.HistoricalData> historyMap = marketDataService
                    .getHistoricalDataBatch(
                            new ArrayList<>(symbols),
                            fromDate,
                            toDate,
                            TimeFrame.DAY, // We want Daily candles to find the close
                            false,
                            null,
                            null,
                            false,
                            false, // Cache is fine
                            false // Keep this historical lookup local to cache/database
                    );

            if (historyMap != null) {
                currentQuotes.forEach((symbol, quote) -> {
                    // Clear the daily base first: if the requested timeframe has no
                    // historical reference, never leave a misleading 1D base behind.
                    quote.setPreviousClose(0.0);
                    var history = findHistory(historyMap, symbol);
                    if (history != null) {
                        if (history.getDataPoints() != null && !history.getDataPoints().isEmpty()) {
                            var points = history.getDataPoints();
                            // Use the latest session on/before the timeframe boundary. The
                            // last array item may be several days after the boundary and
                            // would make the displayed period return too small or too large.
                            var refPoint = points.stream()
                                    .filter(point -> point != null && point.getTime() != null
                                            && !point.getTime().toLocalDate().isAfter(from))
                                    .max(Comparator.comparing(point -> point.getTime().toLocalDate()))
                                    .orElseGet(() -> points.stream()
                                            .filter(point -> point != null && point.getTime() != null)
                                            .min(Comparator.comparing(point -> point.getTime().toLocalDate()))
                                            .orElse(null));
                            if (refPoint == null) {
                                return;
                            }

                            Double pastClose = refPoint.getClose();
                            if (pastClose != null && Double.isFinite(pastClose) && pastClose > 0.0) {
                                quote.setPreviousClose(pastClose);
                            } else {
                                log.warn("getSmartQuotes", "No valid {} comparison close for {}", timeFrame, symbol);
                            }

                            // Recalculate Change & PChange logic is usually in EnrichedStockData or
                            // implicitly in UI.
                            // But OHLCQuote object itself doesn't hold 'change'/'pChange' fields explicitly
                            // standardly?
                            // Wait, StockDataEnricher calculates change = lastPrice - previousClose.
                            // So by updating previousClose here, we effectively update the change
                            // calculation!
                        }
                    }
                });
            }

        } catch (Exception e) {
            log.error("getSmartQuotes", "Error fetching historical reference for timeframe: " + timeFrame, e);
        }

        return currentQuotes;
    }

    private LocalDate getStartDateForTimeFrame(LocalDate current, TimeFrame timeFrame) {
        switch (timeFrame) {
            case WEEK:
                return current.minusWeeks(1);
            case MONTH:
                return current.minusMonths(1);
            case THREE_MONTH:
                return current.minusMonths(3);
            case SIX_MONTH:
                return current.minusMonths(6);
            case YEAR:
                return current.minusYears(1);
            case FIVE_YEAR:
                return current.minusYears(5);
            default:
                return current.minusDays(1);
        }
    }

    /**
     * Get the latest quotes for a list of symbols.
     * Strategy:
     * 1. Try Cache/DB (forceRefresh=false).
     * 2. For missing symbols, try fetching latest Historical Data from DB.
     * 3. Construct a fallback quote using Historical Close as Previous Close.
     */
    public Map<String, OHLCQuote> getSmartQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Try Cache & Database (No Provider)
        // Check local cache/database first. Recover remaining current prices with
        // the provider's batched LTP endpoint, never its per-symbol candle endpoint.
        Map<String, OHLCQuote> rawQuotes = marketDataService.getOHLC(
                symbols, TimeFrame.DAY, false, null, false);
        Map<String, OHLCQuote> quotes = new HashMap<>();

        if (rawQuotes != null) {
            for (Map.Entry<String, OHLCQuote> entry : rawQuotes.entrySet()) {
                OHLCQuote q = entry.getValue();
                if (q != null && q.getLastPrice() > 0.0) {
                    // Keep a valid LTP even when its comparison base is missing;
                    // the bounded local history lookup below can repair the base.
                    // The enricher will reject it if no trustworthy base is found.
                    quotes.put(entry.getKey(), q);
                }
            }
        }

        /*
         * Keep the original cache keys in the returned map because callers may rely
         * on them (for example, NSE:RELIANCE). Only use a canonical identity for
         * the internal missing-symbol check. A bare request must be satisfied by
         * its exchange-qualified cache entry instead of needlessly querying history.
         */
        Set<String> resolvedSymbols = quotes.keySet().stream()
                .map(this::canonicalTradingSymbol)
                .filter(symbol -> !symbol.isBlank())
                .collect(Collectors.toSet());
        Set<String> missingSymbols = symbols.stream()
                .filter(Objects::nonNull)
                .filter(symbol -> !resolvedSymbols.contains(canonicalTradingSymbol(symbol)))
                .collect(Collectors.toSet());

        if (missingSymbols.isEmpty()) {
            return quotes;
        }

        log.info("getSmartQuotes", "Recovering {} missing current quotes with one batched LTP request",
                missingSymbols.size());
        recoverMissingCurrentQuotes(missingSymbols, quotes);

        return quotes;
    }

    private void recoverMissingCurrentQuotes(Set<String> missingSymbols, Map<String, OHLCQuote> quotes) {
        if (stringRedisTemplate == null) {
            log.warn("getSmartQuotes", "Redis quote-recovery coordination is unavailable; skipping provider batch for {} symbols",
                    missingSymbols.size());
            return;
        }

        String lockKey = "market:movers:refresh:" + hashSymbols(missingSymbols);
        String ownerToken = UUID.randomUUID().toString();
        boolean lockAcquired;
        try {
            lockAcquired = Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, ownerToken, Duration.ofSeconds(Math.max(5, refreshLockTtlSeconds))));
        } catch (Exception e) {
            // Fail closed: a Redis outage must not turn every app pod into a provider caller.
            log.warn("getSmartQuotes", "Could not acquire shared quote-recovery lease; skipping provider batch", e);
            return;
        }

        if (!lockAcquired) {
            waitForSharedQuoteRecovery(missingSymbols, quotes);
            return;
        }

        try {
            List<EquityPrice> recoveredPrices = marketDataService.getLivePrices(
                    new ArrayList<>(missingSymbols), null, true);
            if (recoveredPrices == null || recoveredPrices.isEmpty()) {
                log.warn("getSmartQuotes", "Batched LTP recovery returned no quotes for {} requested symbols",
                        missingSymbols.size());
                return;
            }

            int recoveredCount = 0;
            for (EquityPrice price : recoveredPrices) {
                if (price == null || price.getSymbol() == null || !Double.isFinite(price.getLastPrice())
                        || price.getLastPrice() <= 0.0) {
                    continue;
                }
                String returnedSymbol = canonicalTradingSymbol(price.getSymbol());
                String requestedSymbol = missingSymbols.stream()
                        .filter(symbol -> canonicalTradingSymbol(symbol).equals(returnedSymbol))
                        .findFirst()
                        .orElse(null);
                if (requestedSymbol == null) {
                    log.warn("getSmartQuotes", "Batched LTP returned an unrequested or ambiguous symbol; skipping it");
                    continue;
                }

                double previousClose = 0.0;
                OHLCQuote.OHLC ohlc = null;
                if (price.getOhlcv() != null) {
                    previousClose = price.getOhlcv().getClose() != null
                            ? price.getOhlcv().getClose() : 0.0;
                    ohlc = OHLCQuote.OHLC.builder()
                            .open(valueOrZero(price.getOhlcv().getOpen()))
                            .high(valueOrZero(price.getOhlcv().getHigh()))
                            .low(valueOrZero(price.getOhlcv().getLow()))
                            .close(valueOrZero(price.getOhlcv().getClose()))
                            .build();
                }
                OHLCQuote quote = OHLCQuote.builder()
                        .lastPrice(price.getLastPrice())
                        .previousClose(previousClose)
                        .ohlc(ohlc)
                        .build();
                quotes.put(requestedSymbol, quote);
                recoveredCount++;

                // Persist only verified positive LTPs; a later mover request and
                // overlapping index can reuse this shared cache entry.
                if (marketDataCacheService != null) {
                    marketDataCacheService.cacheLatestPrices(Map.of(requestedSymbol, quote));
                }
            }
            log.info("getSmartQuotes", "Batched LTP recovery returned valid prices for {}/{} symbols",
                    recoveredCount, missingSymbols.size());
        } catch (Exception e) {
            log.warn("getSmartQuotes", "Batched LTP recovery failed for {} symbols", missingSymbols.size(), e);
        } finally {
            try {
                stringRedisTemplate.execute(RELEASE_REFRESH_LOCK, List.of(lockKey), ownerToken);
            } catch (Exception e) {
                // The lease still expires automatically; never delete another pod's lease.
                log.warn("getSmartQuotes", "Could not release owned quote-recovery lease; it will expire by TTL");
            }
        }
    }

    private void waitForSharedQuoteRecovery(Set<String> symbols, Map<String, OHLCQuote> quotes) {
        long deadline = System.currentTimeMillis() + Math.max(0, refreshLockWaitMs);
        do {
            try {
                Thread.sleep(Math.min(100, Math.max(1, deadline - System.currentTimeMillis())));
                Map<String, OHLCQuote> cached = marketDataCacheService.getOHLCFromCache(
                        new ArrayList<>(symbols), TimeFrame.DAY);
                if (cached != null) {
                    cached.forEach((symbol, quote) -> {
                        if (quote != null && Double.isFinite(quote.getLastPrice()) && quote.getLastPrice() > 0.0) {
                            quotes.put(symbol, quote);
                        }
                    });
                }
                if (symbols.stream().allMatch(symbol -> quotes.keySet().stream()
                        .anyMatch(key -> canonicalTradingSymbol(key).equals(canonicalTradingSymbol(symbol))))) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("getSmartQuotes", "Interrupted while waiting for shared quote recovery");
                return;
            } catch (Exception e) {
                log.warn("getSmartQuotes", "Could not reread cache while another pod recovers quotes", e);
                return;
            }
        } while (System.currentTimeMillis() < deadline);
        log.warn("getSmartQuotes", "Shared quote recovery did not populate all {} symbols before wait timeout",
                symbols.size());
    }

    private String hashSymbols(Set<String> symbols) {
        String normalized = symbols.stream()
                .map(this::canonicalTradingSymbol)
                .filter(symbol -> !symbol.isBlank())
                .sorted()
                .collect(Collectors.joining(","));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest, 0, 12);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private double valueOrZero(Double value) {
        return value != null && Double.isFinite(value) && value > 0.0 ? value : 0.0;
    }

    private com.am.common.investment.model.historical.HistoricalData findHistory(
            Map<String, com.am.common.investment.model.historical.HistoricalData> historyMap, String symbol) {
        com.am.common.investment.model.historical.HistoricalData history = historyMap.get(symbol);
        if (history != null) {
            return history;
        }
        String clean = canonicalTradingSymbol(symbol);
        history = historyMap.get(clean);
        if (history == null) history = historyMap.get("NSE_EQ:" + clean);
        if (history == null) history = historyMap.get("NSE:" + clean);
        return history;
    }

    /**
     * Makes common AM exchange prefixes comparable without changing an API key.
     */
    private String canonicalTradingSymbol(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        while (normalized.contains(":")) {
            String prefix = normalized.substring(0, normalized.indexOf(':'));
            if (!Set.of("NSE", "BSE", "NSE_EQ", "BSE_EQ").contains(prefix)) {
                break;
            }
            normalized = normalized.substring(normalized.indexOf(':') + 1).trim();
        }
        return normalized;
    }

    private void backfillPreviousCloseForDay(Map<String, OHLCQuote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }

        // A baseline equal to LTP may be a real flat day, or it may be today's
        // close copied into previousClose. Verify it independently before ranking.
        quotes.forEach((symbol, quote) -> {
            if (hasSamePositiveLastAndPreviousClose(quote)) {
                log.info("backfillPreviousCloseForDay",
                        "Verifying unchanged-looking previous close against history symbol={}", symbol);
                quote.setPreviousClose(0.0);
            }
        });

        List<String> symbolsNeedingPrevClose = quotes.entrySet().stream()
                .filter(e -> needsPreviousClose(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (symbolsNeedingPrevClose.isEmpty()) {
            return;
        }

        // Snapshot tradeDate identifies the trading session the baseline applies to.
        // Do not reuse a positive value from an earlier session as today's baseline.
        LocalDate expectedTradeDate = resolveCurrentMarketTradeDate();

        // First attempt: use only a snapshot verified for the current market session.
        if (previousCloseRepository != null && expectedTradeDate != null) {
            try {
                List<String> cleanSymbols = symbolsNeedingPrevClose.stream()
                        .map(this::canonicalTradingSymbol)
                        .distinct()
                        .collect(Collectors.toList());
                List<com.am.marketdata.service.model.PreviousCloseDocument> docs = previousCloseRepository.findBySymbolIn(cleanSymbols);
                if (docs == null || docs.isEmpty()) {
                    docs = previousCloseRepository.findAllById(cleanSymbols);
                }
                if (docs != null && !docs.isEmpty()) {
                    Map<String, Double> closeMap = docs.stream()
                            .filter(d -> d.getPreviousClose() != null && Double.isFinite(d.getPreviousClose())
                                    && d.getPreviousClose() > 0
                                    && expectedTradeDate.toString().equals(d.getTradeDate()))
                            .collect(Collectors.toMap(
                                    d -> canonicalTradingSymbol(d.getSymbol()),
                                    com.am.marketdata.service.model.PreviousCloseDocument::getPreviousClose,
                                    (v1, v2) -> v1));
                    quotes.forEach((symbol, quote) -> {
                        if (needsPreviousClose(quote)) {
                            String clean = canonicalTradingSymbol(symbol);
                            Double prevClose = closeMap.get(clean);
                            if (prevClose != null && prevClose > 0
                                    && Double.compare(prevClose, quote.getLastPrice()) != 0) {
                                quote.setPreviousClose(prevClose);
                                try {
                                    if (marketDataCacheService != null && quote.getLastPrice() > 0) {
                                        marketDataCacheService.cacheLatestPrices(Map.of(symbol, quote));
                                    }
                                } catch (Exception cacheEx) {
                                    log.warn("backfillPreviousCloseForDay", "Failed to cache snapshot previousClose for " + symbol, cacheEx);
                                }
                            }
                        }
                    });
                }
            } catch (Exception repoEx) {
                log.warn("backfillPreviousCloseForDay", "Failed to query previousCloseRepository: " + repoEx.getMessage());
            }
        } else if (previousCloseRepository != null) {
            log.warn("backfillPreviousCloseForDay",
                    "Market calendar did not provide a trusted session date; ignoring stored previous-close snapshots");
        }

        List<String> stillNeeding = quotes.entrySet().stream()
                .filter(e -> needsPreviousClose(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (stillNeeding.isEmpty()) {
            return;
        }

        log.info("backfillPreviousCloseForDay", "Backfilling previousClose for " + stillNeeding.size() + " symbols");

        try {
            ZoneId exchangeZone = ZoneId.of("Asia/Kolkata");
            LocalDate to = LocalDate.now(exchangeZone);
            LocalDate from = to.minusDays(7); // Fetch 7 days of daily history to safely cover holidays/weekends
            Date fromDate = Date.from(from.atStartOfDay(exchangeZone).toInstant());
            Date toDate = Date.from(to.plusDays(1).atStartOfDay(exchangeZone).toInstant());

            Map<String, com.am.common.investment.model.historical.HistoricalData> historyMap = marketDataService
                    .getHistoricalDataBatch(
                            stillNeeding,
                            fromDate,
                            toDate,
                            TimeFrame.DAY,
                            false,
                            null,
                            null,
                            false,
                            false, // forceRefresh=false
                            false // Cache/DB fallback only, no provider calls
                    );

            if (historyMap != null) {
                // Create a normalized map for easier lookup (ignoring exchange prefix)
                Map<String, com.am.common.investment.model.historical.HistoricalData> normalizedHistory = new HashMap<>();
                historyMap.forEach((k, v) -> {
                    String cleanKey = k.contains(":") ? k.substring(k.indexOf(":") + 1) : k;
                    normalizedHistory.put(cleanKey.toUpperCase(), v);
                });

                quotes.forEach((symbol, quote) -> {
                    if (needsPreviousClose(quote)) {
                        String cleanSymbol = symbol.contains(":") ? symbol.substring(symbol.indexOf(":") + 1) : symbol;
                        var history = normalizedHistory.get(cleanSymbol.toUpperCase());
                        if (history != null && history.getDataPoints() != null && !history.getDataPoints().isEmpty()) {
                            var points = history.getDataPoints();
                            // If last candle date is today's session, yesterday is penultimate.
                            // If today's candle is not yet in history, last candle IS yesterday's close.
                            var lastPoint = points.get(points.size() - 1);
                            LocalDate lastPointDate = lastPoint.getTime() != null ? lastPoint.getTime().toLocalDate() : null;
                            LocalDate sessionDate = expectedTradeDate != null ? expectedTradeDate : LocalDate.now(exchangeZone);

                            double prevClose = 0.0;
                            if (lastPointDate != null && lastPointDate.equals(sessionDate)) {
                                prevClose = points.size() >= 2 ? points.get(points.size() - 2).getClose() : 0.0;
                            } else {
                                prevClose = lastPoint.getClose();
                            }

                            if (Double.isFinite(prevClose) && prevClose > 0) {
                                quote.setPreviousClose(prevClose);
                                try {
                                    if (marketDataCacheService != null && quote.getLastPrice() > 0) {
                                        marketDataCacheService.cacheLatestPrices(Map.of(symbol, quote));
                                    }
                                } catch (Exception cacheEx) {
                                    log.warn("backfillPreviousCloseForDay", "Failed to cache backfilled quote for " + symbol, cacheEx);
                                }
                                log.debug("backfillPreviousCloseForDay", "Backfilled previousClose for " + symbol + ": " + prevClose);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("backfillPreviousCloseForDay", "Error backfilling previousClose", e);
        }
    }

    private boolean needsPreviousClose(OHLCQuote quote) {
        return quote != null && (!Double.isFinite(quote.getPreviousClose()) || quote.getPreviousClose() <= 0.0);
    }

    private boolean hasSamePositiveLastAndPreviousClose(OHLCQuote quote) {
        return quote != null && Double.isFinite(quote.getLastPrice()) && quote.getLastPrice() > 0.0
                && Double.isFinite(quote.getPreviousClose()) && quote.getPreviousClose() > 0.0
                && Double.compare(quote.getLastPrice(), quote.getPreviousClose()) == 0;
    }

    /**
     * Finds the latest session date that applies to quotes now. On weekends and
     * holidays, this resolves to the last open session. A stale calendar cannot
     * validate a snapshot, so callers fall back to historical data instead.
     */
    private LocalDate resolveCurrentMarketTradeDate() {
        if (marketCalendarService == null) {
            return null;
        }

        LocalDate candidate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        for (int daysBack = 0; daysBack < 15; daysBack++, candidate = candidate.minusDays(1)) {
            try {
                MarketCalendarService.SessionTiming timing = marketCalendarService.getTimings("NSE", candidate);
                if (timing == null || timing.meta() == null || timing.meta().stale()) {
                    log.warn("backfillPreviousCloseForDay",
                            "Market calendar is missing or stale for {}; refusing snapshot validation", candidate);
                    return null;
                }
                if (timing.open()) {
                    return candidate;
                }
            } catch (Exception calendarEx) {
                log.warn("backfillPreviousCloseForDay",
                        "Could not resolve market session date; refusing snapshot validation", calendarEx);
                return null;
            }
        }

        log.warn("backfillPreviousCloseForDay", "No open NSE session found in the previous 15 days");
        return null;
    }
}
