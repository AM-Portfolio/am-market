package com.am.marketdata.service;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.InstrumentIdentity;
import com.am.marketdata.service.MarketDataService;
import com.am.marketdata.service.calendar.MarketCalendarService;

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
 * Combines cache and database reads with a coalesced provider recovery for
 * missing current quotes.
 */
@Service
@RequiredArgsConstructor
public class SmartStockService {

    private static final DefaultRedisScript<List> ACQUIRE_REFRESH_LOCKS = new DefaultRedisScript<>(
            "local acquired = {} "
                    + "for i, key in ipairs(KEYS) do "
                    + "if redis.call('set', key, ARGV[1], 'NX', 'EX', ARGV[2]) then "
                    + "table.insert(acquired, i) end end return acquired",
            List.class);
    private static final DefaultRedisScript<Long> RELEASE_REFRESH_LOCKS = new DefaultRedisScript<>(
            "local released = 0 "
                    + "for _, key in ipairs(KEYS) do "
                    + "if redis.call('get', key) == ARGV[1] then "
                    + "released = released + redis.call('del', key) end end return released",
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
    private com.am.marketdata.service.repo.SecurityRepository securityRepository;

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

        return applyTimeFrameBase(currentQuotes, symbols, timeFrame);
    }

    /**
     * Movers can provide each member's ticker and ISIN from the index roster.
     * Carry both through the same cache-first and single-batch recovery flow.
     */
    public Map<String, OHLCQuote> getSmartQuotesForIdentities(List<InstrumentIdentity> instruments,
            TimeFrame timeFrame) {
        if (instruments == null || instruments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> symbols = instruments.stream()
                .filter(Objects::nonNull)
                .map(InstrumentIdentity::symbol)
                .filter(symbol -> !symbol.isBlank())
                .distinct()
                .toList();
        Map<String, OHLCQuote> quotes = getQuotesFromCacheAndDatabase(symbols);
        Set<String> resolved = quotes.keySet().stream()
                .map(this::canonicalTradingSymbol)
                .collect(Collectors.toSet());
        List<InstrumentIdentity> missingIdentities = instruments.stream()
                .filter(Objects::nonNull)
                .filter(identity -> !resolved.contains(canonicalTradingSymbol(identity.symbol())))
                .toList();
        if (!missingIdentities.isEmpty()) {
            missingIdentities = validateMemberIdentities(missingIdentities);
            log.info("getSmartQuotesForIdentities",
                    "Recovering {} missing member prices with one ISIN-aware OHLC batch",
                    missingIdentities.size());
            recoverMissingCurrentQuotes(missingIdentities, quotes);
        }
        return applyTimeFrameBase(quotes, symbols, timeFrame);
    }

    /**
     * Only trust a roster ISIN for provider-key construction when AM's security
     * registry confirms the same ticker/ISIN pair. This prevents a stale roster
     * value from assigning another instrument's quote to the requested ticker.
     */
    private List<InstrumentIdentity> validateMemberIdentities(List<InstrumentIdentity> identities) {
        List<InstrumentIdentity> identitiesWithValidIsin = identities.stream()
                .filter(identity -> identity.isin().matches("^[A-Z]{2}[A-Z0-9]{10}$"))
                .toList();
        if (identitiesWithValidIsin.isEmpty()) {
            return identities;
        }
        if (securityRepository == null) {
            log.warn("getSmartQuotesForIdentities",
                    "Security registry is unavailable; disabling ISIN fallback for {} missing members",
                    identitiesWithValidIsin.size());
            return identities.stream()
                    .map(identity -> identity.isin().matches("^[A-Z]{2}[A-Z0-9]{10}$")
                            ? new InstrumentIdentity(identity.symbol(), "", identity.exchange()) : identity)
                    .toList();
        }

        Map<String, List<com.am.marketdata.service.model.security.SecurityDocument>> documentsBySymbol = new HashMap<>();
        List<String> symbols = identitiesWithValidIsin.stream().map(InstrumentIdentity::symbol).distinct().toList();
        try {
            // Keep Mongo $in lists bounded for broad indexes such as NIFTY 500.
            for (int start = 0; start < symbols.size(); start += 200) {
                List<String> chunk = symbols.subList(start, Math.min(start + 200, symbols.size()));
                List<com.am.marketdata.service.model.security.SecurityDocument> found =
                        securityRepository.findBySymbolIn(chunk);
                if (found == null) {
                    continue;
                }
                for (var document : found) {
                    if (document == null || document.getKey() == null || document.getKey().getSymbol() == null) {
                        continue;
                    }
                    documentsBySymbol.computeIfAbsent(canonicalTradingSymbol(document.getKey().getSymbol()),
                            ignored -> new ArrayList<>()).add(document);
                }
            }
        } catch (Exception e) {
            log.warn("getSmartQuotesForIdentities",
                    "Could not verify member ticker/ISIN pairs; using ticker-only resolution for this recovery batch",
                    e);
            return identities.stream()
                    .map(identity -> identity.isin().matches("^[A-Z]{2}[A-Z0-9]{10}$")
                            ? new InstrumentIdentity(identity.symbol(), "", identity.exchange()) : identity)
                    .toList();
        }

        List<InstrumentIdentity> validated = new ArrayList<>(identities.size());
        for (InstrumentIdentity identity : identities) {
            if (!identity.isin().matches("^[A-Z]{2}[A-Z0-9]{10}$")) {
                validated.add(identity);
                continue;
            }
            List<com.am.marketdata.service.model.security.SecurityDocument> matches =
                    documentsBySymbol.getOrDefault(canonicalTradingSymbol(identity.symbol()), List.of());
            Set<String> verifiedIsins = matches.stream()
                    .filter(document -> document.getKey().getIsin() != null)
                    .map(document -> document.getKey().getIsin().trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            boolean inactive = matches.stream().anyMatch(document -> {
                var metadata = document.getMetadata();
                String status = metadata == null ? null : metadata.getStatus();
                return "DELISTED".equalsIgnoreCase(status) || "SUSPENDED".equalsIgnoreCase(status);
            });
            if (verifiedIsins.size() == 1 && verifiedIsins.contains(identity.isin()) && !inactive) {
                validated.add(identity);
            } else {
                String reason = inactive ? "security is inactive"
                        : verifiedIsins.isEmpty() ? "security ISIN is missing"
                        : verifiedIsins.size() > 1 ? "security identity is ambiguous"
                        : "roster and security registry ISIN do not match";
                log.warn("getSmartQuotesForIdentities",
                        "ISIN fallback disabled for {} because {}", identity.symbol(), reason);
                validated.add(new InstrumentIdentity(identity.symbol(), "", identity.exchange()));
            }
        }
        return validated;
    }

    private Map<String, OHLCQuote> applyTimeFrameBase(Map<String, OHLCQuote> currentQuotes,
            List<String> symbols, TimeFrame timeFrame) {

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
     * 2. For missing symbols, request a bounded current OHLC quote batch.
     * 3. Use a verified previous close from that quote or local history.
     */
    public Map<String, OHLCQuote> getSmartQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, OHLCQuote> quotes = getQuotesFromCacheAndDatabase(symbols);
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

        log.info("getSmartQuotes", "Recovering {} missing current quotes with one batched OHLC request",
                missingSymbols.size());
        List<InstrumentIdentity> tickerOnly = missingSymbols.stream()
                .map(this::tickerOnlyIdentity)
                .toList();
        recoverMissingCurrentQuotes(tickerOnly, quotes);
        return quotes;
    }

    private Map<String, OHLCQuote> getQuotesFromCacheAndDatabase(List<String> symbols) {
        /*
         * A successful LTP recovery is stored in market:latest-price:*. Read that
         * cache first. Starting only from intraday bars made a recovered quote look
         * missing after the intraday key expired, so Movers repeated provider work.
         */
        Map<String, OHLCQuote> quotes = new LinkedHashMap<>();
        try {
            Map<String, OHLCQuote> latestQuotes = marketDataCacheService.getLatestPrices(
                    new LinkedHashSet<>(symbols));
            addValidQuotes(quotes, latestQuotes);
        } catch (Exception e) {
            // Redis is an acceleration layer. Continue to the cache/database OHLC path.
            log.warn("getSmartQuotes", "Latest-price cache unavailable; continuing with cache/database lookup", e);
        }

        // Read intraday/cache/database data without a provider fallback. These
        // values may fill missing fields but must not replace a newer LTP cache entry.
        Map<String, OHLCQuote> rawQuotes = marketDataService.getOHLC(
                symbols, TimeFrame.DAY, false, null, false);
        addMissingQuoteFields(quotes, rawQuotes);

        return quotes;
    }

    private InstrumentIdentity tickerOnlyIdentity(String requestSymbol) {
        String normalized = requestSymbol == null ? "" : requestSymbol.trim().toUpperCase(Locale.ROOT);
        String exchange = normalized.startsWith("BSE:") || normalized.startsWith("BSE_EQ:") ? "BSE" : "NSE";
        return new InstrumentIdentity(canonicalTradingSymbol(normalized), "", exchange);
    }

    private void addValidQuotes(Map<String, OHLCQuote> destination, Map<String, OHLCQuote> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (Map.Entry<String, OHLCQuote> entry : candidates.entrySet()) {
            OHLCQuote quote = entry.getValue();
            if (quote == null || !Double.isFinite(quote.getLastPrice()) || quote.getLastPrice() <= 0.0) {
                continue;
            }
            // Keep a valid LTP even when its comparison base is missing; the
            // bounded local history lookup repairs the base before ranking.
            destination.put(entry.getKey(), quote);
        }
    }

    private void addMissingQuoteFields(Map<String, OHLCQuote> destination, Map<String, OHLCQuote> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (Map.Entry<String, OHLCQuote> entry : candidates.entrySet()) {
            OHLCQuote candidate = entry.getValue();
            if (candidate == null || !Double.isFinite(candidate.getLastPrice()) || candidate.getLastPrice() <= 0.0) {
                continue;
            }
            String matchingKey = destination.keySet().stream()
                    .filter(key -> canonicalTradingSymbol(key).equals(canonicalTradingSymbol(entry.getKey())))
                    .findFirst()
                    .orElse(null);
            if (matchingKey == null) {
                destination.put(entry.getKey(), candidate);
                continue;
            }
            OHLCQuote cached = destination.get(matchingKey);
            if (cached.getPreviousClose() <= 0.0 && candidate.getPreviousClose() > 0.0) {
                cached.setPreviousClose(candidate.getPreviousClose());
            }
            if (cached.getOhlc() == null && candidate.getOhlc() != null) {
                cached.setOhlc(candidate.getOhlc());
            }
        }
    }

    private void recoverMissingCurrentQuotes(List<InstrumentIdentity> missingIdentities,
            Map<String, OHLCQuote> quotes) {
        Set<String> missingSymbols = missingIdentities.stream()
                .map(InstrumentIdentity::symbol)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (stringRedisTemplate == null) {
            log.warn("getSmartQuotes", "Redis quote-recovery coordination is unavailable; skipping provider batch for {} symbols",
                    missingSymbols.size());
            return;
        }

        String ownerToken = UUID.randomUUID().toString();
        List<InstrumentIdentity> orderedIdentities = missingIdentities.stream()
                .filter(identity -> identity != null && !identity.symbol().isBlank())
                .collect(Collectors.toMap(
                        identity -> identity.exchange() + "|" + identity.symbol(),
                        identity -> identity,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(InstrumentIdentity::exchange).thenComparing(InstrumentIdentity::symbol))
                .toList();
        List<String> lockKeys = orderedIdentities.stream().map(this::refreshLockKey).toList();
        List<?> acquiredIndexes;
        try {
            // One Redis script claims all free per-instrument leases atomically.
            // Overlapping requests can fetch their disjoint remainder without
            // duplicating work for members another pod is already recovering.
            acquiredIndexes = stringRedisTemplate.execute(
                    ACQUIRE_REFRESH_LOCKS,
                    lockKeys,
                    ownerToken,
                    String.valueOf(Math.max(5, refreshLockTtlSeconds)));
        } catch (Exception e) {
            // Fail closed: a Redis outage must not turn every app pod into a provider caller.
            log.warn("getSmartQuotes", "Could not acquire shared per-symbol quote leases; skipping provider batch", e);
            return;
        }

        Set<Integer> ownedIndexes = new HashSet<>();
        if (acquiredIndexes != null) {
            for (Object index : acquiredIndexes) {
                try {
                    ownedIndexes.add(Integer.parseInt(index.toString()) - 1);
                } catch (NumberFormatException ignored) {
                    log.warn("getSmartQuotes", "Redis returned an invalid quote lease index; skipping that lease result");
                }
            }
        }

        List<InstrumentIdentity> ownedIdentities = new ArrayList<>();
        List<String> ownedLockKeys = new ArrayList<>();
        Set<String> waitingSymbols = new LinkedHashSet<>();
        for (int i = 0; i < orderedIdentities.size(); i++) {
            if (ownedIndexes.contains(i)) {
                ownedIdentities.add(orderedIdentities.get(i));
                ownedLockKeys.add(lockKeys.get(i));
            } else {
                waitingSymbols.add(orderedIdentities.get(i).symbol());
            }
        }

        if (ownedIdentities.isEmpty()) {
            waitForSharedQuoteRecovery(waitingSymbols, quotes);
            return;
        }

        try {
            Map<String, OHLCQuote> recoveredQuotes = marketDataService.getOHLCForIdentities(
                    ownedIdentities, null);
            if (recoveredQuotes == null || recoveredQuotes.isEmpty()) {
                log.warn("getSmartQuotes", "Batched OHLC recovery returned no quotes for {} leased symbols",
                        ownedIdentities.size());
                return;
            }

            int recoveredCount = 0;
            Set<String> ownedSymbols = ownedIdentities.stream().map(InstrumentIdentity::symbol)
                    .collect(Collectors.toSet());
            for (Map.Entry<String, OHLCQuote> entry : recoveredQuotes.entrySet()) {
                OHLCQuote quote = entry.getValue();
                if (quote == null || entry.getKey() == null || !Double.isFinite(quote.getLastPrice())
                        || quote.getLastPrice() <= 0.0) {
                    continue;
                }
                String returnedSymbol = canonicalTradingSymbol(entry.getKey());
                String requestedSymbol = ownedSymbols.stream()
                        .filter(symbol -> canonicalTradingSymbol(symbol).equals(returnedSymbol))
                        .findFirst()
                        .orElse(null);
                if (requestedSymbol == null) {
                    log.warn("getSmartQuotes", "Batched OHLC returned an unrequested or ambiguous symbol; skipping it");
                    continue;
                }

                quotes.put(requestedSymbol, quote);
                recoveredCount++;

                // Persist only verified positive LTPs; a later mover request and
                // overlapping index can reuse this shared cache entry.
                if (marketDataCacheService != null) {
                    marketDataCacheService.cacheLatestPrices(Map.of(requestedSymbol, quote));
                }
            }
            log.info("getSmartQuotes", "Batched OHLC recovery returned valid prices for {}/{} leased symbols",
                    recoveredCount, ownedIdentities.size());
        } catch (Exception e) {
            log.warn("getSmartQuotes", "Batched OHLC recovery failed for {} leased symbols",
                    ownedIdentities.size(), e);
        } finally {
            try {
                stringRedisTemplate.execute(RELEASE_REFRESH_LOCKS, ownedLockKeys, ownerToken);
            } catch (Exception e) {
                // The lease still expires automatically; never delete another pod's lease.
                log.warn("getSmartQuotes", "Could not release owned quote-recovery leases; they will expire by TTL");
            }
        }

        if (!waitingSymbols.isEmpty()) {
            waitForSharedQuoteRecovery(waitingSymbols, quotes);
        }
    }

    private void waitForSharedQuoteRecovery(Set<String> symbols, Map<String, OHLCQuote> quotes) {
        long deadline = System.currentTimeMillis() + Math.max(0, refreshLockWaitMs);
        do {
            try {
                Thread.sleep(Math.min(100, Math.max(1, deadline - System.currentTimeMillis())));
                // Provider recovery writes market:latest-price, not intraday bars.
                // Re-read the same cache that the owner pod populated.
                Map<String, OHLCQuote> cached = marketDataCacheService.getLatestPrices(symbols);
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

    private String refreshLockKey(InstrumentIdentity identity) {
        String normalized = identity.exchange() + "|" + canonicalTradingSymbol(identity.symbol());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return "market:movers:refresh:" + java.util.HexFormat.of().formatHex(digest, 0, 12);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
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
                            // The snapshot trade date is the evidence that this is the
                            // right session baseline; equality with LTP is a valid flat day.
                            if (prevClose != null && Double.isFinite(prevClose) && prevClose > 0) {
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
