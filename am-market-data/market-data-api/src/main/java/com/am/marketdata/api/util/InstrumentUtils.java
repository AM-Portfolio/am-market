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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstrumentUtils {

    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final UpstoxInstrumentRepository upstoxInstrumentRepository;
    private final UnresolvedSymbolRepository unresolvedSymbolRepository;

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

        // Normalize all raw requested symbols to uppercase and strip exchange prefixes (e.g. NSE:RELIANCE -> RELIANCE)
        List<String> upperRawSymbols = rawSymbols.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(s -> s.contains(":") ? s.substring(s.indexOf(":") + 1) : s)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Set<String> candidateSymbols = new HashSet<>();

        if (!fetchIndexStocks) {
            // fetchIndexStocks=false means the caller already knows these are regular stock symbols.
            // No MongoDB lookup needed — just add them all directly.
            log.debug("fetchIndexStocks=false, returning normalized symbols: {}", upperRawSymbols);
            candidateSymbols.addAll(upperRawSymbols);
        } else {
            // fetchIndexStocks=true means some symbols might be index names (e.g. "NIFTY50").
            // OPTIMIZATION: Instead of calling findByIndexSymbol() once per symbol in a loop
            // (which would fire N individual MongoDB queries), we do ONE batch query to fetch
            // all known index documents at once, then expand constituents in memory.
            Set<String> upperSymbolSet = new HashSet<>(upperRawSymbols);
            Map<String, StockIndicesMarketData> indexDocsBySymbol = new HashMap<>();
            try {
                List<StockIndicesMarketData> indexDocs = stockIndicesMarketDataService.findByIndexSymbols(upperSymbolSet);
                if (indexDocs != null) {
                    for (StockIndicesMarketData doc : indexDocs) {
                        if (doc != null && doc.getIndexSymbol() != null) {
                            indexDocsBySymbol.put(doc.getIndexSymbol().toUpperCase(), doc);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to batch-fetch index symbols from MongoDB, falling back to treating all as regular symbols: {}", e.getMessage());
            }

            // Now resolve each symbol: if it is a known index, expand its constituents;
            // otherwise treat it as a regular stock symbol.
            for (String symbol : upperRawSymbols) {
                StockIndicesMarketData indexData = indexDocsBySymbol.get(symbol);
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

        // ONE batch query to find which of our candidate symbols are index symbols.
        // This is needed so we can skip Upstox instrument validation for index symbols
        // (they don't exist in the instruments table, only in the stock-indices collection).
        Set<String> foundIndexSymbols = Collections.emptySet();
        try {
            List<StockIndicesMarketData> indexDocs = stockIndicesMarketDataService.findByIndexSymbols(new HashSet<>(candidateSymbols));
            if (indexDocs != null) {
                foundIndexSymbols = indexDocs.stream()
                        .filter(Objects::nonNull)
                        .map(StockIndicesMarketData::getIndexSymbol)
                        .filter(Objects::nonNull)
                        .map(String::toUpperCase)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("Failed to batch query stock indices from MongoDB", e);
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
                        .collect(Collectors.toList());

                if (!symbolCandidates.isEmpty()) {
                    List<UpstoxInstrument> validInstruments = upstoxInstrumentRepository.findByTradingSymbolIn(symbolCandidates);
                    if (validInstruments != null) {
                        validInstruments.forEach(inst -> {
                            if (inst.getTradingSymbol() != null) {
                                validTradingSymbols.add(inst.getTradingSymbol().toUpperCase());
                            }
                        });
                    }
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

        // Whitelist of valid index constituent symbols that may be missing from the local Upstox instruments table
        java.util.Set<String> whitelist = java.util.Set.of("TATAMOTORS", "MOTHERSON", "BIRLACORPN", "NUVAMA", "GSPL", "MCX", "ANGELONE");

        for (String sym : candidateSymbols) {
            String upper = sym.toUpperCase();
            if (matchingIndices.contains(upper) || whitelist.contains(upper)) {
                resolvedSymbols.add(sym);
            } else if (isinToTicker.containsKey(upper)) {
                // Upstox quotes by trading symbol, not ISIN
                resolvedSymbols.add(isinToTicker.get(upper));
            } else if (validTradingSymbols.contains(sym) || validTradingSymbols.contains(upper)) {
                resolvedSymbols.add(sym);
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

