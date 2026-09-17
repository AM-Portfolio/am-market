package com.am.marketdata.provider.upstox.resolver;

import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.provider.InstrumentDataProvider;
import com.am.marketdata.provider.common.InstrumentContext;
import com.am.marketdata.provider.resolver.SymbolResolver;
import com.am.marketdata.provider.upstox.UpstoxIndexIdentifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Upstox-specific symbol resolver.
 * Resolves trading symbols to Upstox instrument keys.
 * Handles both index symbols and equity symbols.
 */
@Component
public class UpstoxSymbolResolver implements SymbolResolver {

    private final AppLogger log = AppLogger.getLogger();

    private InstrumentDataProvider instrumentDataProvider;

    private final UpstoxIndexIdentifier indexIdentifier;

    public UpstoxSymbolResolver(
            @Qualifier("upstoxInstrumentService") InstrumentDataProvider instrumentDataProvider,
            UpstoxIndexIdentifier indexIdentifier) {
        this.instrumentDataProvider = instrumentDataProvider;
        this.indexIdentifier = indexIdentifier;
    }

    @Override
    public InstrumentContext resolveContext(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new InstrumentContext(new ArrayList<>(), new HashMap<>());
        }

        // 1. Identify and resolve known Indices
        Map<String, String> resolvedIndices = indexIdentifier.resolveIndices(symbols); // Symbol -> Key

        // 2. Identify remaining symbols to lookup in DB, whitelisting direct keys (containing '|')
        List<String> symbolsForDb = new ArrayList<>();
        for (String s : symbols) {
            if (resolvedIndices.containsKey(s)) {
                continue;
            }
            if (s != null && (s.startsWith("GLOBAL_INDEX") || s.contains("|"))) {
                // If it's already an instrument key or a global index key, resolve it directly
                resolvedIndices.put(s, s.replace(":", "|"));
                continue;
            }
            symbolsForDb.add(s);
        }

        if (!symbolsForDb.isEmpty()) {
            log.info("UpstoxSymbolResolver",
                    "Symbols not resolved as indices (will lookup in DB): " + symbolsForDb);
        }

        // 3. Lookup remaining symbols with exchange awareness
        List<com.am.marketdata.common.model.UpstoxInstrument> dbInstruments = resolveInstruments(symbolsForDb);

        // 4. Combine both sources
        List<String> instrumentKeys = new ArrayList<>();
        Map<String, String> keyToSymbolMap = new HashMap<>();

        // Add DB Instruments
        if (dbInstruments != null) {
            instrumentKeys.addAll(dbInstruments.stream()
                    .map(com.am.marketdata.common.model.UpstoxInstrument::getInstrumentKey)
                    .collect(Collectors.toList()));

            // Build reverse lookup from instrument key to trading symbol or original input
            for (com.am.marketdata.common.model.UpstoxInstrument inst : dbInstruments) {
                String tradingSymbol = inst.getTradingSymbol();
                String exchange = inst.getExchange();
                String instrumentKey = inst.getInstrumentKey();

                // Check if any requested symbol matches this instrument's exchange and trading symbol
                String matchedSymbol = null;
                for (String req : symbols) {
                    if (req.equalsIgnoreCase(tradingSymbol)
                            || req.equalsIgnoreCase(exchange + ":" + tradingSymbol)
                            || req.equalsIgnoreCase(inst.getSegment() + ":" + tradingSymbol)
                            || req.equalsIgnoreCase(instrumentKey)) {
                        matchedSymbol = req;
                        break;
                    }
                }

                if (matchedSymbol != null) {
                    keyToSymbolMap.put(instrumentKey, matchedSymbol);
                } else {
                    keyToSymbolMap.put(instrumentKey, tradingSymbol);
                }
            }
        }

        // Add Mapped Indices
        if (resolvedIndices != null) {
            for (Map.Entry<String, String> entry : resolvedIndices.entrySet()) {
                String symbol = entry.getKey();
                String key = entry.getValue();

                if (!instrumentKeys.contains(key)) {
                    instrumentKeys.add(key);
                    keyToSymbolMap.put(key, symbol);
                }
            }
        }

        log.info("UpstoxSymbolResolver",
                String.format("Resolved %d symbols to %d instrument keys", symbols.size(), instrumentKeys.size()));

        return new InstrumentContext(instrumentKeys, keyToSymbolMap);
    }

    private static final int MAX_RESOLUTION_CACHE_SIZE = 5000;
    private final Map<String, com.am.marketdata.common.model.UpstoxInstrument> resolutionCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Resolve instruments from database with in-memory caching and single-query batching.
     */
    private List<com.am.marketdata.common.model.UpstoxInstrument> resolveInstruments(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new ArrayList<>();
        }

        List<com.am.marketdata.common.model.UpstoxInstrument> results = new ArrayList<>();
        List<String> uncachedSymbols = new ArrayList<>();

        // 1. Fast Path: Check in-memory resolution cache first (0 ms)
        for (String s : symbols) {
            String cacheKey = s != null ? s.trim().toUpperCase() : "";
            com.am.marketdata.common.model.UpstoxInstrument cached = resolutionCache.get(cacheKey);
            if (cached != null) {
                results.add(cached);
            } else {
                uncachedSymbols.add(s);
            }
        }

        if (uncachedSymbols.isEmpty()) {
            log.info("UpstoxSymbolResolver", "All " + symbols.size() + " symbols resolved from in-memory JVM cache (0ms)");
            return results;
        }

        // 2. Parse uncached symbols into ISINs and trading symbols
        List<String> allTradingSymbols = new ArrayList<>();
        List<String> isinSymbols = new ArrayList<>();

        for (String s : uncachedSymbols) {
            String cleaned = s;
            if (cleaned.contains("|")) {
                cleaned = cleaned.substring(cleaned.indexOf("|") + 1);
            } else if (cleaned.contains(":")) {
                String[] parts = cleaned.split(":", 2);
                cleaned = parts[1].trim().toUpperCase();
            }

            if (cleaned.matches("^[A-Z]{2}[A-Z0-9]{10}$")) {
                isinSymbols.add(cleaned);
            } else {
                allTradingSymbols.add(cleaned);
            }
        }

        // 3. Single Batched Query by ISIN
        if (!isinSymbols.isEmpty()) {
            log.info("UpstoxSymbolResolver", "Querying DB by ISIN for " + isinSymbols.size() + " symbols in 1 query");
            com.am.marketdata.common.dto.InstrumentSearchCriteria criteria =
                    new com.am.marketdata.common.dto.InstrumentSearchCriteria();
            criteria.setIsins(isinSymbols);
            criteria.setProvider("UPSTOX");
            List<?> found = (List<?>) instrumentDataProvider.searchInstruments(criteria);
            if (found != null) {
                for (Object item : found) {
                    com.am.marketdata.common.model.UpstoxInstrument inst = (com.am.marketdata.common.model.UpstoxInstrument) item;
                    results.add(inst);
                    if (inst.getIsin() != null) {
                        cacheInstrument(inst.getIsin().trim().toUpperCase(), inst);
                    }
                }
            }
        }

        // 4. Single Batched Query by Trading Symbols (across all exchanges in 1 DB call instead of N calls)
        if (!allTradingSymbols.isEmpty()) {
            log.info("UpstoxSymbolResolver", "Querying DB for " + allTradingSymbols.size() + " trading symbols in 1 batched query");
            com.am.marketdata.common.dto.InstrumentSearchCriteria criteria =
                    new com.am.marketdata.common.dto.InstrumentSearchCriteria();
            criteria.setTradingSymbols(allTradingSymbols);
            criteria.setProvider("UPSTOX");
            List<?> found = (List<?>) instrumentDataProvider.searchInstruments(criteria);
            if (found != null) {
                for (Object item : found) {
                    com.am.marketdata.common.model.UpstoxInstrument inst = (com.am.marketdata.common.model.UpstoxInstrument) item;
                    results.add(inst);
                    if (inst.getTradingSymbol() != null) {
                        cacheInstrument(inst.getTradingSymbol().trim().toUpperCase(), inst);
                        if (inst.getExchange() != null) {
                            cacheInstrument(inst.getExchange().trim().toUpperCase() + ":" + inst.getTradingSymbol().trim().toUpperCase(), inst);
                        }
                    }
                }
            }
        }

        return results;
    }

    private void cacheInstrument(String key, com.am.marketdata.common.model.UpstoxInstrument inst) {
        if (key != null && !key.isEmpty() && inst != null) {
            if (resolutionCache.size() >= MAX_RESOLUTION_CACHE_SIZE) {
                resolutionCache.clear(); // Safe LRU purge bound
            }
            resolutionCache.put(key, inst);
        }
    }

    @Override
    public String getProviderName() {
        return "UPSTOX";
    }
}
