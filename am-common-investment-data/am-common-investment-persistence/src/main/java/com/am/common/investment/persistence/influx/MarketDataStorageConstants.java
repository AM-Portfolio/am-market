package com.am.common.investment.persistence.influx;

/**
 * Canonical Influx measurements used by market-data writers and readers.
 * Keeping these names in one place prevents write/read measurement drift.
 */
public final class MarketDataStorageConstants {
    public static final String EQUITY_MEASUREMENT = "equity";
    public static final String INDIAN_INDEX_MEASUREMENT = "market_index";

    private MarketDataStorageConstants() {
        // Constants only.
    }
}
