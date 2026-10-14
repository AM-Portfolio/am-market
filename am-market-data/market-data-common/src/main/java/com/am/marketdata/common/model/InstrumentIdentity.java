package com.am.marketdata.common.model;

import java.util.Locale;

/**
 * Internal lookup identity for a traded instrument. The ticker remains the
 * application/cache key; ISIN is an optional stable identifier for providers.
 */
public record InstrumentIdentity(String symbol, String isin, String exchange) {

    public InstrumentIdentity {
        symbol = normalize(symbol);
        isin = normalize(isin);
        exchange = exchange == null || exchange.isBlank()
                ? "NSE"
                : exchange.trim().toUpperCase(Locale.ROOT);
        if ("NSE_EQ".equals(exchange)) {
            exchange = "NSE";
        } else if ("BSE_EQ".equals(exchange)) {
            exchange = "BSE";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
