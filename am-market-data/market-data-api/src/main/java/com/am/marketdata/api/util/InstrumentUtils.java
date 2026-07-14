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

        // Normalize all raw requested symbols to uppercase
        List<String> upperRawSymbols = rawSymbols.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Set<String> candidateSymbols = new HashSet<>();

        if (!fetchIndexStocks) {
            // If fetchIndexStocks is false, return normalized symbols as-is
            log.debug("fetchIndexStocks=false, returning normalized symbols: {}", upperRawSymbols);
            candidateSymbols.addAll(upperRawSymbols);
        } else {
            // Expand indices if fetchIndexStocks is true
            for (String symbol : upperRawSymbols) {
                try {
                    StockIndicesMarketData indexData = stockIndicesMarketDataService.findByIndexSymbol(symbol);
                    if (indexData != null && indexData.getData() != null) {
                        candidateSymbols.add(symbol); // Keep index symbol itself
                        List<String> constituents = indexData.getData().stream()
                                .map(data -> data.getSymbol().toUpperCase())
                                .collect(Collectors.toList());
                        candidateSymbols.addAll(constituents);
                        log.debug("Resolved index {} to itself + {} stocks", symbol, constituents.size());
                    } else {
                        candidateSymbols.add(symbol);
                    }
                } catch (Exception e) {
                    log.warn("Error resolving symbol {}, treating as regular symbol: {}", symbol, e.getMessage());
                    candidateSymbols.add(symbol);
                }
            }
        }

        // Batch validate candidates against DB (excluding indices)
        List<String> nonIndexCandidates = candidateSymbols.stream()
                .filter(sym -> stockIndicesMarketDataService.findByIndexSymbol(sym) == null)
                .collect(Collectors.toList());

        Set<String> validTradingSymbols = new HashSet<>();
        if (!nonIndexCandidates.isEmpty()) {
            try {
                List<UpstoxInstrument> validInstruments = upstoxInstrumentRepository.findByTradingSymbolIn(nonIndexCandidates);
                if (validInstruments != null) {
                    validTradingSymbols = validInstruments.stream()
                            .map(UpstoxInstrument::getTradingSymbol)
                            .map(String::toUpperCase)
                            .collect(Collectors.toSet());
                }
            } catch (Exception e) {
                log.error("Failed to batch query upstock_instruments for validation", e);
                // Fallback: If DB query fails, don't drop symbols to avoid complete API outage
                return candidateSymbols;
            }
        }

        Set<String> resolvedSymbols = new HashSet<>();
        List<String> unresolvedSymbols = new ArrayList<>();

        // Whitelist of valid index constituent symbols that may be missing from the local Upstox instruments table
        java.util.Set<String> whitelist = java.util.Set.of("TATAMOTORS", "MOTHERSON", "BIRLACORPN", "NUVAMA", "GSPL", "MCX", "ANGELONE");

        for (String sym : candidateSymbols) {
            if (stockIndicesMarketDataService.findByIndexSymbol(sym) != null || 
                validTradingSymbols.contains(sym) || 
                whitelist.contains(sym.toUpperCase())) {
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
}

