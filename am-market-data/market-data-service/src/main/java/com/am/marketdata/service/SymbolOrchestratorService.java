package com.am.marketdata.service;

import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.service.client.ParserApiClient;
import com.am.common.investment.model.stockindice.StockData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Service for retrieving ISIN (International Securities Identification Number)
 * data
 * Used by schedulers and streamer to get the list of symbols to process
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymbolOrchestratorService {

    private final ParserApiClient parserApiClient;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;

    @Value("${scheduler.symbols.default:RELIANCE}")
    private String defaultSymbols;

    private List<String> cachedSymbols = null;

    /**
     * Get Nifty 500 Symbols
     * Fetches constituents of NIFTY 500 from source.
     */
    public List<String> getNifty500Symbols() {
        return getIndexSymbols(Set.of("NIFTY 500"));
    }

    /**
     * Get ETF Symbols
     * Fetches all ETFs from parser.
     */
    public List<String> getEtfSymbols() {
        return parserApiClient.getAllEtfSymbols();
    }

    /**
     * Get Index Symbols
     * Fetches constituents for key indices
     */
    public List<String> getIndexSymbols(Set<String> indicesToCheck) {
        Set<String> constituents = new HashSet<>();

        for (String index : indicesToCheck) {
            try {
                var indexData = stockIndicesMarketDataService.findByIndexSymbol(index);
                if (indexData != null && indexData.getData() != null) {
                    constituents.addAll(indexData.getData().stream()
                            .map(StockData::getSymbol)
                            .collect(Collectors.toList()));
                }
            } catch (Exception e) {
                log.error("Failed to fetch constituents for index: {}", index, e);
            }
        }
        return constituents.stream().toList();
    }

    /**
     * Find all distinct symbols for processing.
     * Aggregates symbols from various sources.
     * Uses caching to prevent repeated calls to external services.
     * 
     * @return List of all symbols
     */
    public synchronized List<String> findDistinctIsins() {
        if (cachedSymbols != null && !cachedSymbols.isEmpty()) {
            log.info("Returning cached symbols. Count: {}", cachedSymbols.size());
            return cachedSymbols;
        }

        log.info("Cache miss. Fetching and aggregating symbols...");

        // 1. Start with default symbols from config
        List<String> combinedSymbols = new ArrayList<>();
        if (defaultSymbols != null && !defaultSymbols.isEmpty()) {
            combinedSymbols.addAll(List.of(defaultSymbols.split(",")));
        }

        // 2. Add Nifty 500 symbols
        // combinedSymbols.addAll(getNifty500Symbols());

        // 3. Add ETF symbols
        // combinedSymbols.addAll(getEtfSymbols());

        // Test
        combinedSymbols.addAll(List.of("RELIANCE"));

        // 5. Deduplicate and return
        cachedSymbols = combinedSymbols.stream()
                .distinct()
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toList());

        log.info("Symbol aggregation complete. Total unique symbols: {}", cachedSymbols.size());
        return cachedSymbols;
    }

    /**
     * Refresh the symbol cache
     * Forces a re-fetch of symbols from all sources
     */
    public synchronized void refreshSymbolCache() {
        log.info("Refreshing symbol cache...");
        cachedSymbols = null;
        findDistinctIsins();
    }
}
