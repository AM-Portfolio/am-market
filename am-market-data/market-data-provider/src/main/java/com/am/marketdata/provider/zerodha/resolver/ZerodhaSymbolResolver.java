package com.am.marketdata.provider.zerodha.resolver;

import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.common.provider.InstrumentDataProvider;
import com.am.marketdata.provider.common.InstrumentContext;
import com.am.marketdata.provider.resolver.SymbolResolver;
import com.am.marketdata.provider.zerodha.model.ZerodhaInstrument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Zerodha-specific symbol resolver.
 * Resolves trading symbols to Zerodha instrument tokens.
 */
@Component
public class ZerodhaSymbolResolver implements SymbolResolver {

    private final AppLogger log = AppLogger.getLogger();

    private final InstrumentDataProvider instrumentDataProvider;

    public ZerodhaSymbolResolver(
            @Qualifier("zerodhaInstrumentService") InstrumentDataProvider instrumentDataProvider) {
        this.instrumentDataProvider = instrumentDataProvider;
    }

    @Override
    public InstrumentContext resolveContext(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new InstrumentContext(new ArrayList<>(), new HashMap<>());
        }

        List<ZerodhaInstrument> instruments = resolveInstruments(symbols);

        List<String> instrumentKeys = new ArrayList<>();
        Map<String, String> keyToSymbolMap = new HashMap<>();

        if (instruments != null && !instruments.isEmpty()) {
            // For Zerodha, instrument key is the instrument token (as String)
            instrumentKeys.addAll(instruments.stream()
                    .map(inst -> String.valueOf(inst.getInstrumentToken()))
                    .collect(Collectors.toList()));

            keyToSymbolMap.putAll(instruments.stream()
                    .collect(Collectors.toMap(
                            inst -> String.valueOf(inst.getInstrumentToken()),
                            ZerodhaInstrument::getTradingSymbol,
                            (existing, replacement) -> existing)));
        }

        log.info("ZerodhaSymbolResolver",
                String.format("Resolved %d symbols to %d instrument keys", symbols.size(), instrumentKeys.size()));

        return new InstrumentContext(instrumentKeys, keyToSymbolMap);
    }

    /**
     * Resolve instruments from database
     */
    private List<ZerodhaInstrument> resolveInstruments(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new ArrayList<>();
        }

        // Strip exchange prefix if present (e.g., NSE:RELIANCE -> RELIANCE)
        List<String> cleanedSymbols = symbols.stream()
                .map(s -> {
                    if (s.startsWith("NSE:") || s.startsWith("BSE:")) {
                        return s.substring(4);
                    }
                    return s;
                })
                .collect(Collectors.toList());

        com.am.marketdata.common.dto.InstrumentSearchCriteria criteria = new com.am.marketdata.common.dto.InstrumentSearchCriteria();
        criteria.setTradingSymbols(cleanedSymbols);
        criteria.setProvider("ZERODHA");

        return (List<ZerodhaInstrument>) instrumentDataProvider
                .searchInstruments(criteria);
    }

    @Override
    public String getProviderName() {
        return "ZERODHA";
    }
}
