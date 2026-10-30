package com.am.marketdata.provider.upstox.resolver;

import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.model.InstrumentIdentity;
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

    private final com.am.marketdata.provider.upstox.repo.SymbolAliasRepository symbolAliasRepository;

    private final Map<String, String> isinRemapCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> symbolAliasCache = new java.util.concurrent.ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public UpstoxSymbolResolver(
            @Qualifier("upstoxInstrumentService") InstrumentDataProvider instrumentDataProvider,
            UpstoxIndexIdentifier indexIdentifier,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.am.marketdata.provider.upstox.repo.SymbolAliasRepository symbolAliasRepository) {
        this.instrumentDataProvider = instrumentDataProvider;
        this.indexIdentifier = indexIdentifier;
        this.symbolAliasRepository = symbolAliasRepository;
        loadAliases();
    }

    public UpstoxSymbolResolver(
            InstrumentDataProvider instrumentDataProvider,
            UpstoxIndexIdentifier indexIdentifier) {
        this(instrumentDataProvider, indexIdentifier, null);
    }

    @jakarta.annotation.PostConstruct
    public void loadAliases() {
        if (symbolAliasRepository == null) {
            return;
        }
        try {
            List<com.am.marketdata.common.model.SymbolAlias> aliases = symbolAliasRepository.findAll();
            if (aliases != null) {
                isinRemapCache.clear();
                symbolAliasCache.clear();
                for (com.am.marketdata.common.model.SymbolAlias alias : aliases) {
                    if (alias.getId() == null || alias.getTarget() == null) {
                        continue;
                    }
                    if ("ISIN".equalsIgnoreCase(alias.getType())) {
                        isinRemapCache.put(alias.getId().trim().toUpperCase(), alias.getTarget().trim().toUpperCase());
                    } else {
                        symbolAliasCache.put(alias.getId().trim().toUpperCase(), alias.getTarget().trim().toUpperCase());
                    }
                }
                log.info("UpstoxSymbolResolver", "Loaded " + isinRemapCache.size() + " ISIN remaps and " + symbolAliasCache.size() + " symbol aliases from DB");
            }
        } catch (Exception e) {
            log.warn("UpstoxSymbolResolver", "Could not load symbol aliases from DB: " + e.getMessage());
        }
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
            String normalized = s == null ? null : s.trim();
            if (normalized != null && resolvedIndices.containsKey(normalized)) {
                continue;
            }
            if (normalized != null && (normalized.startsWith("GLOBAL_INDEX") || normalized.contains("|"))) {
                // If it's already an instrument key or a global index key, resolve it directly
                resolvedIndices.put(normalized, normalized.replace(":", "|"));
                continue;
            }
            if (normalized != null && !normalized.isEmpty()) {
                symbolsForDb.add(normalized);
            }
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

    /**
     * Resolve index members by ticker first, then use their validated ISIN when
     * the provider instrument master is missing or has a conflicting mapping.
     */
    @Override
    public InstrumentContext resolveContextForIdentities(List<InstrumentIdentity> instruments) {
        if (instruments == null || instruments.isEmpty()) {
            return resolveContext(List.of());
        }

        List<InstrumentIdentity> validIdentities = instruments.stream()
                .filter(java.util.Objects::nonNull)
                .filter(identity -> !identity.symbol().isBlank())
                .toList();
        InstrumentContext context = resolveContext(validIdentities.stream()
                .map(identity -> identity.exchange() + ":" + identity.symbol())
                .toList());

        for (InstrumentIdentity identity : validIdentities) {
            String isin = identity.isin();
            if (!isin.matches("^[A-Z]{2}[A-Z0-9]{10}$")) {
                log.warn("UpstoxSymbolResolver", "No valid ISIN for {}: using ticker resolution only",
                        identity.symbol());
                continue;
            }

            String segment = identity.exchange() + "_EQ";
            String expectedKey = segment + "|" + isin;
            List<String> matchingKeys = context.getInstrumentKeys().stream()
                    .filter(key -> identity.symbol().equalsIgnoreCase(
                            normalizeTradingSymbol(context.getSymbol(key))))
                    .toList();
            boolean mappedToExpectedIsin = matchingKeys.stream()
                    .anyMatch(key -> key.contains("|")
                            && key.substring(key.indexOf('|') + 1).equalsIgnoreCase(isin));

            if (!mappedToExpectedIsin) {
                // Index membership carries the current ticker-to-ISIN identity. Do
                // not let a stale/missing provider-master row block a valid batch.
                resolutionCache.remove(identity.symbol());
                resolutionCache.remove(identity.exchange() + ":" + identity.symbol());
                for (String key : matchingKeys) {
                    context.getInstrumentKeys().remove(key);
                    context.getKeyToSymbolMap().entrySet().removeIf(entry ->
                            key.equals(entry.getKey()) || key.replace('|', ':').equals(entry.getKey()));
                }
                if (!context.getInstrumentKeys().contains(expectedKey)) {
                    context.getInstrumentKeys().add(expectedKey);
                }
                log.info("UpstoxSymbolResolver",
                        "Resolved {} from its index-member ISIN because the instrument-master mapping was missing or did not match",
                        identity.symbol());
            }

            context.getKeyToSymbolMap().put(expectedKey, identity.symbol());
            context.getKeyToSymbolMap().put(expectedKey.replace('|', ':'), identity.symbol());
            context.getKeyToSymbolMap().put(segment + ":" + identity.symbol(), identity.symbol());
            context.getKeyToSymbolMap().put(identity.exchange() + ":" + identity.symbol(), identity.symbol());
            context.getKeyToSymbolMap().put(identity.symbol(), identity.symbol());
        }

        return context;
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
                    com.am.marketdata.common.model.UpstoxInstrument normalized = normalizeInstrumentKey(inst);
                    if (normalized != null) {
                        candidateInstruments.add(normalized);
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
                    com.am.marketdata.common.model.UpstoxInstrument normalized = normalizeInstrumentKey(inst);
                    if (normalized != null) {
                        candidateInstruments.add(normalized);
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
            // NSE_EQ:PWL → exchange NSE (not NSE_EQ) so scoring matches Mongo exchange=NSE.
            if (reqExchange.endsWith("_EQ") || reqExchange.endsWith("_FO") || reqExchange.endsWith("_INDEX")) {
                reqExchange = reqExchange.substring(0, reqExchange.indexOf('_'));
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

    /**
     * Upstox hist SDK requires {@code SEGMENT|ISIN} keys. Dig/local docs sometimes store
     * bare tickers as instrument_key — rebuild from segment + ISIN when possible.
     */
    private com.am.marketdata.common.model.UpstoxInstrument normalizeInstrumentKey(
            com.am.marketdata.common.model.UpstoxInstrument inst) {
        if (inst == null) {
            return null;
        }
        if (inst.getIsin() != null && isinRemapCache.containsKey(inst.getIsin().trim().toUpperCase())) {
            String activeIsin = isinRemapCache.get(inst.getIsin().trim().toUpperCase());
            inst.setIsin(activeIsin);
            if (inst.getInstrumentKey() != null && inst.getInstrumentKey().contains("|")) {
                String seg = inst.getInstrumentKey().substring(0, inst.getInstrumentKey().indexOf('|'));
                inst.setInstrumentKey(seg + "|" + activeIsin);
            }
        }
        if (inst.getInstrumentKey() != null) {
            for (Map.Entry<String, String> staleEntry : isinRemapCache.entrySet()) {
                if (inst.getInstrumentKey().endsWith("|" + staleEntry.getKey())) {
                    String seg = inst.getInstrumentKey().substring(0, inst.getInstrumentKey().indexOf('|'));
                    inst.setInstrumentKey(seg + "|" + staleEntry.getValue());
                    inst.setIsin(staleEntry.getValue());
                    break;
                }
            }
        }
        String key = inst.getInstrumentKey();
        if (key != null && key.contains("|")) {
            return inst;
        }
        String isin = inst.getIsin();
        if (isin == null || isin.isBlank()) {
            return null;
        }
        String segment = inst.getSegment();
        if (segment == null || segment.isBlank()) {
            String ex = inst.getExchange() != null ? inst.getExchange().toUpperCase() : "NSE";
            segment = ex.endsWith("_EQ") ? ex : ex + "_EQ";
        }
        inst.setInstrumentKey(segment + "|" + isin.trim().toUpperCase());
        return inst;
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
        return symbolAliasCache.getOrDefault(cleaned, cleaned);
    }

    @Override
    public String getProviderName() {
        return "UPSTOX";
    }
}
