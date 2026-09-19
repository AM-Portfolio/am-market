package com.am.marketdata.common.model.marketinfo;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class UpstoxInstrumentKeys {
    private static final Map<String, String> KNOWN_KEYS = Map.of(
            "NIFTY 50", "NSE_INDEX|Nifty 50",
            "NIFTY50", "NSE_INDEX|Nifty 50",
            "NIFTY BANK", "NSE_INDEX|Nifty Bank",
            "NIFTYBANK", "NSE_INDEX|Nifty Bank",
            "BANKNIFTY", "NSE_INDEX|Nifty Bank",
            "BANK NIFTY", "NSE_INDEX|Nifty Bank",
            "SENSEX", "BSE_INDEX|SENSEX"
    );

    private UpstoxInstrumentKeys() {
    }

    public static String mapSymbolToInstrumentKey(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        String trimmed = symbol.trim();
        if (trimmed.contains("|")) {
            return trimmed;
        }
        String normalized = trimmed.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        String known = KNOWN_KEYS.get(normalized);
        if (known != null) {
            return known;
        }
        String titleCase = Arrays.stream(normalized.split(" "))
                .map(word -> word.isEmpty()
                        ? word
                        : word.substring(0, 1) + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
        return "NSE_INDEX|" + titleCase;
    }
}
