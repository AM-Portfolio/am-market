package com.am.marketdata.service.util;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class to determine data retrieval strategy based on configuration
 * flags.
 * Centralizes the logic for choosing retrieval order to ensure consistent
 * behavior
 * across all data retrieval operations (historical data, OHLC, etc.).
 */
public class DataRetrievalStrategyUtil {

    /**
     * Determines the retrieval order based on the forceRefresh flag.
     * 
     * @param forceRefresh If true, skips cache and database, goes directly to
     *                     provider.
     *                     If false, uses normal retrieval order (cache → database →
     *                     provider).
     * @return List of DataSourceType in the order they should be checked
     */
    public static List<DataSourceType> getRetrievalOrder(boolean forceRefresh) {
        if (forceRefresh) {
            // Force refresh: Skip cache and database, go directly to provider
            return Arrays.asList(DataSourceType.PROVIDER);
        } else {
            // Normal flow: Try cache first, then database, then provider
            return Arrays.asList(
                    DataSourceType.CACHE,
                    DataSourceType.DATABASE,
                    DataSourceType.PROVIDER);
        }
    }

    /**
     * Gets a human-readable description of the retrieval strategy.
     * Useful for logging purposes.
     * 
     * @param forceRefresh The force refresh flag
     * @return Description of the strategy
     */
    public static String getStrategyDescription(boolean forceRefresh) {
        if (forceRefresh) {
            return "FORCE_REFRESH (Provider only)";
        } else {
            return "NORMAL (Cache → Database → Provider)";
        }
    }
}
