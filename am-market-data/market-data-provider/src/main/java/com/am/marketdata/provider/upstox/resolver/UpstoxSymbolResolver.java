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
                keyToSymbolMap.put(inst.getInstrumentKey(), inst.getTradingSymbol());
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

    /**
     * Resolve instruments from database with exchange grouping.
     * Detects whether input has an exchange prefix (e.g. BSE:RELIANCE vs NSE:RELIANCE)
     * and filters DB search criteria by the explicit exchange.
     */
    private List<com.am.marketdata.common.model.UpstoxInstrument> resolveInstruments(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new ArrayList<>();
        }

        // Group trading symbols by exchange: Exchange -> List of Clean Trading Symbols
        Map<String, List<String>> symbolsByExchange = new HashMap<>();
        List<String> isinSymbols = new ArrayList<>();

        for (String s : symbols) {
            String exchange = "NSE"; // Default to NSE
            String cleaned = s;

            if (cleaned.contains("|")) {
                cleaned = cleaned.substring(cleaned.indexOf("|") + 1);
            } else if (cleaned.contains(":")) {
                String[] parts = cleaned.split(":", 2);
                exchange = parts[0].trim().toUpperCase();
                cleaned = parts[1].trim().toUpperCase();
            }

            // ISINs are 12-char alphanumeric codes starting with two uppercase letters (e.g. INE, IN2)
            if (cleaned.matches("^[A-Z]{2}[A-Z0-9]{10}$")) {
                isinSymbols.add(cleaned);
            } else {
                symbolsByExchange.computeIfAbsent(exchange, k -> new ArrayList<>()).add(cleaned);
            }
        }

        List<com.am.marketdata.common.model.UpstoxInstrument> results = new ArrayList<>();

        // Query by ISIN
        if (!isinSymbols.isEmpty()) {
            log.info("UpstoxSymbolResolver",
                    "Querying DB by ISIN for " + isinSymbols.size() + " symbols");
            com.am.marketdata.common.dto.InstrumentSearchCriteria criteria =
                    new com.am.marketdata.common.dto.InstrumentSearchCriteria();
            criteria.setIsins(isinSymbols);
            criteria.setProvider("UPSTOX");
            List<?> found = (List<?>) instrumentDataProvider.searchInstruments(criteria);
            if (found != null) {
                found.forEach(i -> results.add((com.am.marketdata.common.model.UpstoxInstrument) i));
            }
        }

        // Query by trading symbol grouped by exchange
        for (Map.Entry<String, List<String>> entry : symbolsByExchange.entrySet()) {
            String exchange = entry.getKey();
            List<String> tradingSymbols = entry.getValue();

            if (!tradingSymbols.isEmpty()) {
                log.info("UpstoxSymbolResolver",
                        String.format("Querying DB for exchange %s with %d symbols", exchange, tradingSymbols.size()));
                com.am.marketdata.common.dto.InstrumentSearchCriteria criteria =
                        new com.am.marketdata.common.dto.InstrumentSearchCriteria();
                criteria.setTradingSymbols(tradingSymbols);
                criteria.setExchanges(List.of(exchange));
                criteria.setProvider("UPSTOX");
                List<?> found = (List<?>) instrumentDataProvider.searchInstruments(criteria);
                if (found != null) {
                    found.forEach(i -> results.add((com.am.marketdata.common.model.UpstoxInstrument) i));
                }
            }
        }

        return results;
    }

    @Override
    public String getProviderName() {
        return "UPSTOX";
    }
}
