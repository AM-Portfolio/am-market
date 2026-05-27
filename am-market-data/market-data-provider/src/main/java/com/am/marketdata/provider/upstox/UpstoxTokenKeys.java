package com.am.marketdata.provider.upstox;

/**
 * Shared Redis key and helpers for Upstox OAuth access tokens.
 */
public final class UpstoxTokenKeys {

    /** Canonical key — login ({@link UpstoxApiService}) writes here. */
    public static final String REDIS_ACCESS_TOKEN = "market_data:upstox:access_token";

    /** Legacy key used by {@link com.am.marketdata.provider.upstox.client.UpStockClient} before unification. */
    public static final String REDIS_ACCESS_TOKEN_LEGACY = "market-data:upstox:access_token";

    private UpstoxTokenKeys() {
    }

    public static boolean isUsable(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return !"<no value>".equalsIgnoreCase(token.trim());
    }
}
