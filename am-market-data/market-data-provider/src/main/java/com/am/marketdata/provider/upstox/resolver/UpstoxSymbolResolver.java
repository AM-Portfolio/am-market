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
import java.util.Set;
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

    private static final Set<String> EXCHANGE_PREFIXES = Set.of(
            "NSE", "BSE", "NSE_EQ", "BSE_EQ", "NSE_FO", "BSE_FO", "MCX");

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
        Map<String, com.am.marketdata.common.model.UpstoxInstrument> dbInstrumentMap = resolveInstrumentsMap(symbolsForDb);

        // 4. Combine both sources
        List<String> instrumentKeys = new ArrayList<>();
        Map<String, String> keyToSymbolMap = new HashMap<>();

        // Add DB Instruments
        if (dbInstrumentMap != null) {
            for (Map.Entry<String, com.am.marketdata.common.model.UpstoxInstrument> entry : dbInstrumentMap.entrySet()) {
                String requestedSymbol = entry.getKey();
                com.am.marketdata.common.model.UpstoxInstrument inst = entry.getValue();
                if (inst == null) continue;

                String instrumentKey = inst.getInstrumentKey();
                if (instrumentKey == null || !instrumentKey.contains("|")) {
                    log.warn("UpstoxSymbolResolver",
                            "Skipping invalid instrument key for symbol " + requestedSymbol + ": " + instrumentKey);
                    continue;
                }

                if (!instrumentKeys.contains(instrumentKey)) {
                    instrumentKeys.add(instrumentKey);
                }

                String tradingSymbol = inst.getTradingSymbol();
                String exchange = inst.getExchange();
                String segment = inst.getSegment();

                // 1. Map exact instrument key (e.g. NSE_EQ|INE002A01018)
                keyToSymbolMap.put(instrumentKey, requestedSymbol);
                // 2. Map colon version of instrument key (e.g. NSE_EQ:INE002A01018)
                keyToSymbolMap.put(instrumentKey.replace("|", ":"), requestedSymbol);

                // 3. Map Upstox response key format segment:tradingSymbol (e.g. NSE_EQ:RELIANCE)
                if (segment != null && tradingSymbol != null) {
                    keyToSymbolMap.put(segment + ":" + tradingSymbol, requestedSymbol);
                    keyToSymbolMap.put(segment + "|" + tradingSymbol, requestedSymbol);
                }

                // 4. Map exchange:tradingSymbol (e.g. NSE:RELIANCE)
                if (exchange != null && tradingSymbol != null) {
                    keyToSymbolMap.put(exchange + ":" + tradingSymbol, requestedSymbol);
                    keyToSymbolMap.put(exchange + "|" + tradingSymbol, requestedSymbol);
                }

                // 5. Map bare trading symbol (e.g. RELIANCE)
                if (tradingSymbol != null) {
                    keyToSymbolMap.put(tradingSymbol, requestedSymbol);
                }
            }
        }

        // Add Mapped Indices
        if (resolvedIndices != null) {
            for (Map.Entry<String, String> entry : resolvedIndices.entrySet()) {
                String symbol = entry.getKey();
                String key = entry.getValue();

                if (key != null && key.contains("|") && !instrumentKeys.contains(key)) {
                    instrumentKeys.add(key);
                    keyToSymbolMap.put(key, symbol);
                    keyToSymbolMap.put(key.replace("|", ":"), symbol);
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
     * Maps each requested symbol to its single best matching UpstoxInstrument.
     */
    private Map<String, com.am.marketdata.common.model.UpstoxInstrument> resolveInstrumentsMap(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, com.am.marketdata.common.model.UpstoxInstrument> results = new HashMap<>();
        List<String> uncachedSymbols = new ArrayList<>();

        // 1. Fast Path: Check in-memory resolution cache first (0 ms)
        for (String s : symbols) {
            String clean = normalizeTradingSymbol(s);
            com.am.marketdata.common.model.UpstoxInstrument cached = resolutionCache.get(s);
            if (cached == null) {
                cached = resolutionCache.get(clean);
            }
            if (cached != null && cached.getInstrumentKey() != null && cached.getInstrumentKey().contains("|")) {
                results.put(s, cached);
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
            String cleaned = s == null ? "" : s.trim().toUpperCase();
            if (cleaned.contains("|")) {
                cleaned = cleaned.substring(cleaned.indexOf("|") + 1);
            } else {
                cleaned = normalizeTradingSymbol(cleaned);
            }

            if (cleaned.matches("^[A-Z]{2}[A-Z0-9]{10}$")) {
                isinSymbols.add(cleaned);
            } else {
                allTradingSymbols.add(cleaned);
            }
        }

        List<com.am.marketdata.common.model.UpstoxInstrument> candidateInstruments = new ArrayList<>();

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
                    if (inst != null && inst.getInstrumentKey() != null && inst.getInstrumentKey().contains("|")) {
                        candidateInstruments.add(inst);
                    }
                }
            }
        }

        // 4. Single Batched Query by Trading Symbols
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
                    if (inst != null && inst.getInstrumentKey() != null && inst.getInstrumentKey().contains("|")) {
                        candidateInstruments.add(inst);
                    }
                }
            }
        }

        // 5. Match each uncached symbol to its best candidate instrument
        for (String reqSymbol : uncachedSymbols) {
            String reqExchange = "NSE";
            String cleanSymbol = reqSymbol;
            if (reqSymbol.contains(":")) {
                String[] parts = reqSymbol.split(":", 2);
                reqExchange = parts[0].trim().toUpperCase();
                cleanSymbol = parts[1].trim();
            }
            cleanSymbol = normalizeTradingSymbol(cleanSymbol);

            com.am.marketdata.common.model.UpstoxInstrument bestInst = null;
            int bestScore = -1;

            for (com.am.marketdata.common.model.UpstoxInstrument candidate : candidateInstruments) {
                String candSymbol = candidate.getTradingSymbol();
                String candIsin = candidate.getIsin();
                boolean symbolMatches = (candSymbol != null && candSymbol.equalsIgnoreCase(cleanSymbol))
                        || (candIsin != null && candIsin.equalsIgnoreCase(cleanSymbol));

                if (!symbolMatches) {
                    continue;
                }

                int score = 0;
                String candEx = candidate.getExchange() != null ? candidate.getExchange().toUpperCase() : "";
                String candSeg = candidate.getSegment() != null ? candidate.getSegment().toUpperCase() : "";

                if (candEx.equalsIgnoreCase(reqExchange) && candSeg.equalsIgnoreCase(reqExchange + "_EQ")) {
                    score = 4;
                } else if (candEx.equalsIgnoreCase(reqExchange)) {
                    score = 3;
                } else if (candSeg.equalsIgnoreCase("NSE_EQ")) {
                    score = 2;
                } else {
                    score = 1;
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestInst = candidate;
                }
            }

            if (bestInst != null) {
                results.put(reqSymbol, bestInst);
                cacheInstrument(reqSymbol, bestInst);
                cacheInstrument(cleanSymbol, bestInst);
                if (bestInst.getTradingSymbol() != null) {
                    cacheInstrument(bestInst.getTradingSymbol().toUpperCase(), bestInst);
                }
                if (bestInst.getIsin() != null) {
                    cacheInstrument(bestInst.getIsin().toUpperCase(), bestInst);
                }
            } else {
                log.warn("UpstoxSymbolResolver", "Could not resolve valid Upstox instrument with pipe key for: " + reqSymbol);
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

    private String normalizeTradingSymbol(String symbol) {
        /*
         * Old watchlists may still publish NSE:NSE:IDEA. Upstox stores IDEA as
         * the trading symbol, so remove repeated known exchange labels first.
         */
        String cleaned = symbol == null ? "" : symbol.trim().toUpperCase();
        while (cleaned.contains(":")) {
            int delimiter = cleaned.indexOf(':');
            String prefix = cleaned.substring(0, delimiter);
            if (!EXCHANGE_PREFIXES.contains(prefix)) {
                break;
            }
            cleaned = cleaned.substring(delimiter + 1).trim();
        }
        return cleaned;
    }

    @Override
    public String getProviderName() {
        return "UPSTOX";
    }
}
