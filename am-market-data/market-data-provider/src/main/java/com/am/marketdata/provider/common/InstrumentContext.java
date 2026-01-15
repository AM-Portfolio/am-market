package com.am.marketdata.provider.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context holding resolved instrument mapping information.
 * Contains both instrument keys and their mappings to trading symbols.
 * Provider-agnostic - can be used by any provider.
 */
public class InstrumentContext {

    public final List<String> instrumentKeys;
    public final Map<String, String> keyToSymbolMap;

    public InstrumentContext(List<String> instrumentKeys, Map<String, String> keyToSymbolMap) {
        this.instrumentKeys = instrumentKeys;
        this.keyToSymbolMap = keyToSymbolMap;
    }

    /**
     * Get all instrument keys
     * 
     * @return List of instrument keys
     */
    public List<String> getInstrumentKeys() {
        return instrumentKeys;
    }

    /**
     * Get instrument key to symbol mapping
     * 
     * @return Map of instrument key to trading symbol
     */
    public Map<String, String> getKeyToSymbolMap() {
        return keyToSymbolMap;
    }

    /**
     * Check if context has any instruments
     * 
     * @return true if instruments exist
     */
    public boolean isEmpty() {
        return instrumentKeys == null || instrumentKeys.isEmpty();
    }

    /**
     * Get symbol for instrument key
     * 
     * @param instrumentKey Instrument key
     * @return Trading symbol or instrument key if not found
     */
    public String getSymbol(String instrumentKey) {
        return keyToSymbolMap.getOrDefault(instrumentKey, instrumentKey);
    }
}
