package com.am.marketdata.provider.resolver;

import com.am.marketdata.provider.common.InstrumentContext;
import java.util.List;

/**
 * Common interface for symbol resolution across all providers.
 * Resolves trading symbols (e.g., "RELIANCE") to provider-specific instrument
 * contexts.
 * 
 * Each provider (Upstox, Zerodha, etc.) implements this interface to:
 * 1. Convert raw symbols to provider-specific instrument keys
 * 2. Query instrument database for mappings
 * 3. Return InstrumentContext with resolved instruments
 */
public interface SymbolResolver {

    /**
     * Resolve trading symbols to InstrumentContext
     * 
     * @param symbols List of trading symbols (e.g., ["RELIANCE", "TCS"])
     * @return InstrumentContext with provider-specific instrument keys
     */
    InstrumentContext resolveContext(List<String> symbols);

    /**
     * Get provider name
     * 
     * @return Provider name (UPSTOX, ZERODHA, etc.)
     */
    String getProviderName();
}
