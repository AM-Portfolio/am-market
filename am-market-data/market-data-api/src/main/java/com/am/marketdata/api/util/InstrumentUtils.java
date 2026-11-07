package com.am.marketdata.api.util;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.marketdata.provider.upstox.repo.UpstoxInstrumentRepository;
import com.am.marketdata.service.repo.UnresolvedSymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstrumentUtils {

    private static final long METADATA_CACHE_TTL_MILLIS = 5 * 60 * 1000L;
    private static final int MAX_METADATA_CACHE_ENTRIES = 10_000;

    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final UpstoxInstrumentRepository upstoxInstrumentRepository;
    private final UnresolvedSymbolRepository unresolvedSymbolRepository;
    private final Map<String, CacheEntry<StockIndicesMarketData>> indexMetadataCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<String>> instrumentSymbolCache = new ConcurrentHashMap<>();

    private record CacheEntry<T>(T value, long expiresAt) {
        boolean valid(long now) {
            return expiresAt > now;
        }
    }

    /**
     * Resolves a comma-separated string of symbols with optional index expansion.
     *
     * @param commaSeparatedSymbols String containing symbols separated by commas.
     * @param fetchIndexStocks      If true, fetch individual stocks from index
     *                              symbols.
     *                              If false, return as-is.
     * @return Set of unique stock symbols.
     */
    public Set<String> resolveSymbols(String commaSeparatedSymbols, boolean fetchIndexStocks) {
        if (commaSeparatedSymbols == null || commaSeparatedSymbols.trim().isEmpty()) {
            return new HashSet<>();
        }
        List<String> rawSymbols = Arrays.stream(commaSeparatedSymbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return resolveSymbols(rawSymbols, fetchIndexStocks);
    }

    /**
     * Resolves a list of symbols with optional index expansion.
     * Checks if regular symbols exist in the Upstox database. Invalid/missing
     * symbols are filtered out, logged, and persisted to unresolved_symbols collection.
     *
     * @param rawSymbols       List of symbols or indices.
     * @param fetchIndexStocks If true, fetch individual stocks from index symbols
     *                         via DB lookup.
     *                         If false, return symbols as-is without DB expansion.
     * @return Set of unique, valid stock symbols.
     */
    public Set<String> resolveSymbols(List<String> rawSymbols, boolean fetchIndexStocks) {
        if (rawSymbols == null || rawSymbols.isEmpty()) {
            return new HashSet<>();
        }

        log.info("[EXCHANGE_DIAG] resolveSymbols called: rawSymbols={} fetchIndexStocks={}",
                rawSymbols, fetchIndexStocks);

        // Normalize raw requested symbols to uppercase.
        // Keep explicit exchange prefixes (e.g. "BSE:TCS") intact so exchange routing is preserved.
        List<String> upperRawSymbols = rawSymbols.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        log.info("[EXCHANGE_DIAG] resolveSymbols normalized: upperRawSymbols={}", upperRawSymbols);

        Set<String> candidateSymbols = new HashSet<>();
        Map<String, StockIndicesMarketData> indexDocsBySymbol = new HashMap<>();

        if (!fetchIndexStocks) {
            // fetchIndexStocks=false means the caller already knows these are index symbols or specific symbols.
            // No MongoDB index expansion needed — just add them all directly as candidates.
            log.debug("fetchIndexStocks=false, returning normalized symbols: {}", upperRawSymbols);
            candidateSymbols.addAll(upperRawSymbols);
        } else {
            // fetchIndexStocks=true means some symbols might be index names (e.g. "NIFTY50", "NSE:NIFTY50").
            // Include both original symbol and base symbol (without prefix) in index search query.
            Set<String> searchSymbols = new HashSet<>(upperRawSymbols);
            for (String s : upperRawSymbols) {
                if (s.contains(":")) {
                    searchSymbols.add(s.substring(s.indexOf(":") + 1));
                }
            }
            try {
                long now = System.currentTimeMillis();
                // Short expiry keeps requests fast while allowing new or changed indexes to appear.
                Set<String> uncachedIndexSymbols = new HashSet<>();
                for (String searchSymbol : searchSymbols) {
                    CacheEntry<StockIndicesMarketData> cached = indexMetadataCache.get(normalizeLookupSymbol(searchSymbol));
                    if (cached != null && cached.valid(now)) {
                        if (cached.value() != null && cached.value().getIndexSymbol() != null) {
                            StockIndicesMarketData value = cached.value();
                            indexDocsBySymbol.put(value.getIndexSymbol().toUpperCase(), value);
                        }
                    } else {
                        uncachedIndexSymbols.add(searchSymbol);
                    }
                }

                List<StockIndicesMarketData> indexDocs = uncachedIndexSymbols.isEmpty()
                        ? Collections.emptyList()
                        : stockIndicesMarketDataService.findByIndexSymbols(uncachedIndexSymbols);
                if (indexDocs != null) {
                    for (StockIndicesMarketData doc : indexDocs) {
                        if (doc != null && doc.getIndexSymbol() != null) {
                            indexDocsBySymbol.put(doc.getIndexSymbol().toUpperCase(), doc);
                            indexMetadataCache.put(normalizeLookupSymbol(doc.getIndexSymbol()),
                                    new CacheEntry<>(doc, now + METADATA_CACHE_TTL_MILLIS));
                        }
                    }
                }
                // Cache misses briefly as well, so a normal equity request does not
                // query the index collection repeatedly. Expiry lets new indexes appear.
                for (String symbol : uncachedIndexSymbols) {
                    String normalized = normalizeLookupSymbol(symbol);
                    if (!indexMetadataCache.containsKey(normalized)) {
                        indexMetadataCache.put(normalized, new CacheEntry<>(null, now + METADATA_CACHE_TTL_MILLIS));
                    }
                }
                trimMetadataCache(indexMetadataCache);
            } catch (Exception e) {
                log.warn("Failed to batch-fetch index symbols from MongoDB, falling back to treating all as regular symbols: {}", e.getMessage());
            }

            // Now resolve each symbol: if it is a known index, expand its constituents;
            // otherwise treat it as a regular stock symbol.
            for (String symbol : upperRawSymbols) {
                String baseSymbol = symbol.contains(":") ? symbol.substring(symbol.indexOf(":") + 1) : symbol;
                StockIndicesMarketData indexData = indexDocsBySymbol.get(symbol);
                if (indexData == null) {
                    indexData = indexDocsBySymbol.get(baseSymbol);
                }
                if (indexData != null && indexData.getData() != null) {
                    // It's an index — keep the index symbol itself and add all constituent stocks
                    candidateSymbols.add(symbol);
                    List<String> constituents = indexData.getData().stream()
                            .map(data -> data.getSymbol().toUpperCase())
                            .collect(Collectors.toList());
                    candidateSymbols.addAll(constituents);
                    log.debug("Resolved index {} to itself + {} constituent stocks", symbol, constituents.size());
                } else {
                    // Regular stock symbol — add as-is
                    candidateSymbols.add(symbol);
                }
            }
        }

        // Reuse index symbols found during initial resolution or preserve requested index symbols when fetchIndexStocks=false.
        Set<String> foundIndexSymbols = new HashSet<>();
        if (!fetchIndexStocks) {
            foundIndexSymbols.addAll(candidateSymbols);
        } else {
            for (String cand : candidateSymbols) {
                String upper = cand.toUpperCase();
                String base = cand.contains(":") ? cand.substring(cand.indexOf(":") + 1).toUpperCase() : upper;
                if (indexDocsBySymbol.containsKey(upper) || indexDocsBySymbol.containsKey(base) || upper.startsWith("GLOBAL_") || upper.startsWith("NSE_")) {
                    foundIndexSymbols.add(upper);
                }
            }
        }
        final Set<String> matchingIndices = foundIndexSymbols;

        // Validate the non-index candidates against the Upstox instruments table in ONE batch query.
        // This filters out any symbols that don't exist in Upstox (typos, delisted stocks, etc.)
        List<String> nonIndexCandidates = candidateSymbols.stream()
                .filter(sym -> !matchingIndices.contains(sym.toUpperCase()))
                .collect(Collectors.toList());

        Set<String> validTradingSymbols = new HashSet<>();
        Map<String, String> isinToTicker = Map.of();
        if (!nonIndexCandidates.isEmpty()) {
            try {
                List<String> isinCandidates = nonIndexCandidates.stream()
                        .filter(InstrumentUtils::looksLikeIsin)
                        .collect(Collectors.toList());
                List<String> symbolCandidates = nonIndexCandidates.stream()
                        .filter(s -> !looksLikeIsin(s))
                        .map(s -> s.contains(":") ? s.substring(s.indexOf(":") + 1).trim() : s)
                        .collect(Collectors.toList());

                if (!symbolCandidates.isEmpty()) {
                    long now = System.currentTimeMillis();
                    // Cache only confirmed instruments. Unknown symbols must still reach MongoDB
                    // so a new listing is not hidden by a negative cache entry.
                    List<String> uncachedSymbols = symbolCandidates.stream()
                            .filter(symbol -> {
                                CacheEntry<String> cached = instrumentSymbolCache.get(normalizeLookupSymbol(symbol));
                                if (cached != null && cached.valid(now)) {
                                    validTradingSymbols.add(cached.value());
                                    return false;
                                }
                                return true;
                            })
                            .toList();
                    List<UpstoxInstrument> validInstruments = uncachedSymbols.isEmpty()
                            ? Collections.emptyList()
                            : upstoxInstrumentRepository.findByTradingSymbolIn(uncachedSymbols);
                    // Null-guard: treat null return same as empty list (e.g. mock/isolated profile)
                    if (validInstruments != null) {
                        validInstruments.forEach(inst -> {
                            if (inst.getTradingSymbol() != null) {
                                String tradingSymbol = inst.getTradingSymbol().toUpperCase();
                                validTradingSymbols.add(tradingSymbol);
                                instrumentSymbolCache.put(normalizeLookupSymbol(tradingSymbol),
                                        new CacheEntry<>(tradingSymbol, now + METADATA_CACHE_TTL_MILLIS));
                            }
                        });
                    }
                    // Fail-open: if instruments DB returned nothing (empty collection or unreachable),
                    // pass all non-ISIN symbol candidates through to prevent silent data suppression.
                    if (validTradingSymbols.isEmpty() && isinCandidates.isEmpty()) {
                        log.warn("[INSTRUMENT_UTILS] upstoxInstrumentRepository returned no results for {} symbols — " +
                                "passing candidates through (fail-open) to avoid empty API response", symbolCandidates.size());
                        validTradingSymbols.addAll(symbolCandidates.stream()
                                .map(String::toUpperCase)
                                .collect(Collectors.toSet()));
                    }
                    trimMetadataCache(instrumentSymbolCache);
                }

                isinToTicker = lookupIsinToTradingSymbol(isinCandidates);
                isinToTicker.values().forEach(validTradingSymbols::add);
            } catch (Exception e) {
                log.error("Failed to batch query upstock_instruments for validation", e);
                // Fallback: if DB fails, pass all symbols through to avoid a complete API outage
                return candidateSymbols;
            }
        }

        Set<String> resolvedSymbols = new HashSet<>();
        List<String> unresolvedSymbols = new ArrayList<>();

        for (String sym : candidateSymbols) {
            String upper = sym.toUpperCase();
            String baseSymbol = sym.contains(":") ? sym.substring(sym.indexOf(":") + 1).trim() : sym;
            String upperBase = baseSymbol.toUpperCase();
            if (upper.startsWith("GLOBAL_") || matchingIndices.contains(upper)) {
                resolvedSymbols.add(sym);
            } else if (isinToTicker.containsKey(upper) || isinToTicker.containsKey(upperBase)) {
                // Upstox quotes by trading symbol, not ISIN
                String ticker = isinToTicker.getOrDefault(upper, isinToTicker.get(upperBase));
                resolvedSymbols.add(sym.contains(":") ? sym.substring(0, sym.indexOf(":") + 1) + ticker : ticker);
            } else if (validTradingSymbols.contains(sym) || validTradingSymbols.contains(upper)
                    || validTradingSymbols.contains(baseSymbol) || validTradingSymbols.contains(upperBase)) {
                if (!sym.contains(":")) {
                    resolvedSymbols.add("NSE_EQ:" + sym.toUpperCase());
                } else {
                    resolvedSymbols.add(sym);
                }
            } else {
                unresolvedSymbols.add(sym);
            }
        }

        // Handle unresolved symbols asynchronously
        if (!unresolvedSymbols.isEmpty()) {
            log.warn("[UNRESOLVED_SYMBOLS] Filtering out {} invalid symbols: {}", unresolvedSymbols.size(), unresolvedSymbols);
            CompletableFuture.runAsync(() -> {
                for (String sym : unresolvedSymbols) {
                    try {
                        unresolvedSymbolRepository.incrementRequestCount(sym);
                    } catch (Exception e) {
                        log.error("Failed to save unresolved symbol: {}", sym, e);
                    }
                }
            });
        }

        log.info("[EXCHANGE_DIAG] resolveSymbols result: {} symbols resolved from {} candidates, unresolved={}",
                resolvedSymbols.size(), candidateSymbols.size(), unresolvedSymbols);
        return resolvedSymbols;
    }

    /**
     * Maps ISIN-shaped inputs to Upstox trading symbols (NSE cash preferred).
     * Non-ISIN inputs are omitted. Fail-open: missing instruments are omitted.
     */
    public Map<String, String> mapIsinsToTradingSymbols(Collection<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }
        List<String> isins = symbols.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(InstrumentUtils::looksLikeIsin)
                .distinct()
                .collect(Collectors.toList());
        return lookupIsinToTradingSymbol(isins);
    }

    /**
     * Copies quote values under the original ISIN key so callers that stored
     * holdings as ISIN still match {@code quotes.get(isin)}.
     */
    public <T> void aliasQuotesUnderOriginalIsins(Collection<String> requested, Map<String, T> quotes) {
        if (requested == null || quotes == null || quotes.isEmpty()) {
            return;
        }
        Map<String, String> isinToTicker = mapIsinsToTradingSymbols(requested);
        for (Map.Entry<String, String> entry : isinToTicker.entrySet()) {
            T quote = quotes.get(entry.getValue());
            if (quote != null) {
                quotes.putIfAbsent(entry.getKey(), quote);
            }
        }
    }

    static boolean looksLikeIsin(String value) {
        return value != null && value.length() == 12 && value.matches("[A-Z]{2}[A-Z0-9]{10}");
    }

    private String normalizeLookupSymbol(String symbol) {
        if (symbol == null) return "";
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        int separator = Math.max(normalized.indexOf(':'), normalized.indexOf('|'));
        return separator >= 0 ? normalized.substring(separator + 1).trim() : normalized;
    }

    private <T> void trimMetadataCache(Map<String, CacheEntry<T>> cache) {
        if (cache.size() > MAX_METADATA_CACHE_ENTRIES) {
            cache.entrySet().removeIf(entry -> !entry.getValue().valid(System.currentTimeMillis()));
        }
        if (cache.size() > MAX_METADATA_CACHE_ENTRIES) {
            cache.clear();
        }
    }

    private Map<String, String> lookupIsinToTradingSymbol(List<String> isinCandidates) {
        if (isinCandidates == null || isinCandidates.isEmpty()) {
            return Map.of();
        }
        List<UpstoxInstrument> instruments;
        try {
            instruments = upstoxInstrumentRepository.findByIsinIn(isinCandidates);
        } catch (Exception e) {
            log.warn("ISIN to trading-symbol lookup failed: {}", e.getMessage());
            return Map.of();
        }
        if (instruments == null || instruments.isEmpty()) {
            return Map.of();
        }
        Map<String, UpstoxInstrument> bestByIsin = new HashMap<>();
        for (UpstoxInstrument inst : instruments) {
            if (inst == null || inst.getIsin() == null || inst.getTradingSymbol() == null
                    || inst.getTradingSymbol().isBlank()) {
                continue;
            }
            String isin = inst.getIsin().trim().toUpperCase();
            UpstoxInstrument existing = bestByIsin.get(isin);
            if (existing == null || rankInstrument(inst) < rankInstrument(existing)) {
                bestByIsin.put(isin, inst);
            }
        }
        Map<String, String> result = new HashMap<>();
        bestByIsin.forEach((isin, inst) -> result.put(isin, inst.getTradingSymbol().trim().toUpperCase()));
        return result;
    }

    /** Lower is better. Prefer NSE cash / GB over BSE; skip F&O. */
    private static int rankInstrument(UpstoxInstrument inst) {
        String exchange = inst.getExchange() != null ? inst.getExchange().trim().toUpperCase() : "";
        String type = inst.getInstrumentType() != null ? inst.getInstrumentType().trim().toUpperCase() : "";
        if (exchange.contains("NFO") || exchange.contains("BFO") || exchange.contains("MCX")
                || type.contains("FUT") || type.contains("OPT")) {
            return 100;
        }
        if (exchange.startsWith("NSE") && (type.contains("EQ") || type.contains("GB") || type.isEmpty())) {
            return 0;
        }
        if (exchange.startsWith("NSE")) {
            return 1;
        }
        if (exchange.startsWith("BSE")) {
            return 2;
        }
        return 3;
    }
}

