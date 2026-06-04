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

        // 2. Identify remaining symbols to lookup in DB
        List<String> symbolsForDb = symbols.stream()
                .filter(s -> !resolvedIndices.containsKey(s))
                .collect(Collectors.toList());

        if (!symbolsForDb.isEmpty()) {
            log.info("UpstoxSymbolResolver",
                    "Symbols not resolved as indices (will lookup in DB): " + symbolsForDb);
        }

        // 3. Lookup remaining symbols
        List<com.am.marketdata.common.model.UpstoxInstrument> dbInstruments = resolveInstruments(symbolsForDb);

        // 4. Combine both sources
        List<String> instrumentKeys = new ArrayList<>();
        Map<String, String> keyToSymbolMap = new HashMap<>();

        // Add DB Instruments
        if (dbInstruments != null) {
            instrumentKeys.addAll(dbInstruments.stream()
                    .map(com.am.marketdata.common.model.UpstoxInstrument::getInstrumentKey)
                    .collect(Collectors.toList()));

            keyToSymbolMap.putAll(dbInstruments.stream()
                    .collect(Collectors.toMap(
                            com.am.marketdata.common.model.UpstoxInstrument::getInstrumentKey,
                            com.am.marketdata.common.model.UpstoxInstrument::getTradingSymbol,
                            (existing, replacement) -> existing)));
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

    /**
     * Resolve instruments from database
     */
    private List<com.am.marketdata.common.model.UpstoxInstrument> resolveInstruments(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new ArrayList<>();
        }

        // Strip exchange prefix if present (e.g., NSE:RELIANCE -> RELIANCE, NSE_EQ:RELIANCE -> RELIANCE)
        List<String> cleanedSymbols = symbols.stream()
                .map(s -> {
                    if (s == null) {
                        return "";
                    }
                    if (s.startsWith("NSE_EQ:") || s.startsWith("BSE_EQ:")) {
                        return s.substring(7);
                    } else if (s.startsWith("NSE:") || s.startsWith("BSE:")) {
                        return s.substring(4);
                    }
                    return s;
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> isins = new ArrayList<>();
        List<String> tradingSymbols = new ArrayList<>();
        for (String s : cleanedSymbols) {
            if (s.toUpperCase().matches("^[A-Z]{2}[A-Z0-9]{9}[0-9]$")) {
                isins.add(s.toUpperCase());
            } else {
                tradingSymbols.add(s);
            }
        }

        List<com.am.marketdata.common.model.UpstoxInstrument> results = new ArrayList<>();

        if (!tradingSymbols.isEmpty()) {
            com.am.marketdata.common.dto.InstrumentSearchCriteria criteria = new com.am.marketdata.common.dto.InstrumentSearchCriteria();
            criteria.setTradingSymbols(tradingSymbols);
            criteria.setProvider("UPSTOX");
            List<com.am.marketdata.common.model.UpstoxInstrument> found = (List<com.am.marketdata.common.model.UpstoxInstrument>) instrumentDataProvider
                    .searchInstruments(criteria);
            if (found != null) {
                results.addAll(found);
            }
        }

        if (!isins.isEmpty()) {
            com.am.marketdata.common.dto.InstrumentSearchCriteria criteria = new com.am.marketdata.common.dto.InstrumentSearchCriteria();
            criteria.setIsins(isins);
            criteria.setProvider("UPSTOX");
            List<com.am.marketdata.common.model.UpstoxInstrument> found = (List<com.am.marketdata.common.model.UpstoxInstrument>) instrumentDataProvider
                    .searchInstruments(criteria);
            if (found != null) {
                results.addAll(found);
            }
        }

        return results;
    }

    @Override
    public String getProviderName() {
        return "UPSTOX";
    }
}
