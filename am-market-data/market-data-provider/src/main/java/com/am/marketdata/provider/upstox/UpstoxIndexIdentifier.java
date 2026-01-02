package com.am.marketdata.provider.upstox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UpstoxIndexIdentifier {

    private final Map<String, String> indexKeyMap = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initialize() {
        log.info("Loading Upstox Index Identifiers...");
        try {
            ClassPathResource resource = new ClassPathResource("upstock_instruments_indices.json");
            if (!resource.exists()) {
                log.warn("upstock_instruments_indices.json not found in classpath");
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                List<Map<String, Object>> indices = objectMapper.readValue(inputStream,
                        new TypeReference<List<Map<String, Object>>>() {
                        });

                for (Map<String, Object> index : indices) {
                    String id = (String) index.get("_id");
                    if (id != null && id.startsWith("NSE_INDEX|")) {
                        // Extract "Nifty 50" from "NSE_INDEX|Nifty 50"
                        String value = id.substring("NSE_INDEX|".length());
                        // Store as UPPERCASE -> Full Key
                        indexKeyMap.put(value.toUpperCase(), id);

                        // Also map the 'name' field if available and different
                        String name = (String) index.get("name");
                        if (name != null && !name.equalsIgnoreCase(value)) {
                            indexKeyMap.put(name.toUpperCase(), id);
                        }
                        // Also map the 'trading_symbol' field if available and different
                        String tradingSymbol = (String) index.get("trading_symbol");
                        if (tradingSymbol != null && !tradingSymbol.equalsIgnoreCase(value)) {
                            indexKeyMap.put(tradingSymbol.toUpperCase(), id);
                        }
                    }
                }
                log.info("Loaded {} Upstox Index Identifiers", indexKeyMap.size());
            }
        } catch (Exception e) {
            log.error("Failed to load Upstox Index Identifiers", e);
        }
    }

    public String getInstrumentKey(String name) {
        if (name == null)
            return null;
        String cleanName = name.toUpperCase();
        if (cleanName.startsWith("NSE:") || cleanName.startsWith("BSE:")) {
            cleanName = cleanName.substring(4);
        }
        return indexKeyMap.get(cleanName);
    }

    /**
     * Resolves a list of symbols to their Upstox instrument keys if they are
     * indices.
     * 
     * @param symbols List of potential index names
     * @return Map of {Original Symbol -> Upstox Instrument Key} for found indices.
     */
    public Map<String, String> resolveIndices(List<String> symbols) {
        Map<String, String> resolved = new HashMap<>();
        if (symbols == null)
            return resolved;

        for (String symbol : symbols) {
            String key = getInstrumentKey(symbol);
            if (key != null) {
                resolved.put(symbol, key);
            }
        }
        return resolved;
    }
}
