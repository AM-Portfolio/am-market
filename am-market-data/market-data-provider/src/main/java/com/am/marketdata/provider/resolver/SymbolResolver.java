package com.am.marketdata.provider.resolver;

import com.am.marketdata.provider.common.InstrumentContext;
import java.util.List;
import com.am.marketdata.common.model.InstrumentIdentity;

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

    /** Resolve known identities while preserving the existing ticker-only API. */
    default InstrumentContext resolveContextForIdentities(List<InstrumentIdentity> instruments) {
        if (instruments == null || instruments.isEmpty()) {
            return resolveContext(List.of());
        }
        return resolveContext(instruments.stream()
                .filter(java.util.Objects::nonNull)
                .map(InstrumentIdentity::symbol)
                .filter(symbol -> !symbol.isBlank())
                .toList());
    }

    /**
     * Get provider name
     * 
     * @return Provider name (UPSTOX, ZERODHA, etc.)
     */
    String getProviderName();
}
