package com.am.marketdata.api.util;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstrumentUtils {

    private final StockIndicesMarketDataService stockIndicesMarketDataService;

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
     *
     * @param rawSymbols       List of symbols or indices.
     * @param fetchIndexStocks If true, fetch individual stocks from index symbols
     *                         via DB lookup.
     *                         If false, return symbols as-is without DB expansion.
     * @return Set of unique stock symbols.
     */
    public Set<String> resolveSymbols(List<String> rawSymbols, boolean fetchIndexStocks) {
        Set<String> finalSymbols = new HashSet<>();
        finalSymbols.addAll(rawSymbols);
        if (!fetchIndexStocks) {
            // If fetchIndexStocks is false, return symbols as-is without database lookup
            log.debug("fetchIndexStocks=false, returning symbols as-is: {}", rawSymbols);
            return finalSymbols;
        }

        // If fetchIndexStocks is true, perform database lookup and expansion
        for (String symbol : rawSymbols) {
            try {
                // Check if the symbol is an index
                StockIndicesMarketData indexData = stockIndicesMarketDataService.findByIndexSymbol(symbol);

                if (indexData != null && indexData.getData() != null) {
                    // It is an index, add the index symbol ITSELF plus all constituents
                    finalSymbols.add(symbol);
                    List<String> constituents = indexData.getData().stream()
                            .map(data -> data.getSymbol())
                            .collect(Collectors.toList());
                    finalSymbols.addAll(constituents);
                    log.debug("Resolved index {} to itself + {} stocks", symbol, constituents.size());
                } else {
                    // Not an index or no data, treat as regular symbol
                    finalSymbols.add(symbol);
                }
            } catch (Exception e) {
                // On error (e.g. not found if service throws), assume it's a regular symbol
                log.warn("Error resolving symbol {}, treating as regular symbol: {}", symbol, e.getMessage());
                finalSymbols.add(symbol);
            }
        }
        return finalSymbols;
    }
}
