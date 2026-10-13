package com.am.marketdata.api.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses comma-separated trading symbols and qualifies them with an exchange/segment prefix.
 */
public final class SymbolParseUtils {

    private SymbolParseUtils() {
    }

    /**
     * Convert comma-separated symbols to a set, prepending {@code exchange} when the symbol
     * has no explicit prefix (e.g. {@code BSE:RELIANCE}, {@code NSE_FO:NIFTY24...}, pipe keys).
     *
     * @param symbols  Comma-separated symbols
     * @param exchange Default exchange/segment for bare symbols (e.g. NSE, BSE, NSE_FO)
     * @return Resolved symbol set
     */
    public static Set<String> parseSymbols(String symbols, String exchange) {
        if (symbols == null || symbols.isEmpty()) {
            return new HashSet<>();
        }

        String defaultExchange = (exchange != null && !exchange.isBlank()) ? exchange.trim().toUpperCase() : "NSE";

        return Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    if (s.contains(":") || s.contains("|")) {
                        return s;
                    }
                    return defaultExchange + ":" + s;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Backward-compatible parse without explicit exchange (defaults to NSE).
     */
    public static Set<String> parseSymbols(String symbols) {
        return parseSymbols(symbols, "NSE");
    }
}
