package com.am.marketdata.provider.common;

import com.marketdata.common.MarketDataProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating market data provider instances based on configuration
 */
@Slf4j
@Component
public class MarketDataProviderFactory {

    private final ApplicationContext applicationContext;

    @Value("${market-data.provider:zerodha}")
    private String activeProvider;

    public MarketDataProviderFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Get the configured market data provider
     * 
     * @return MarketDataProvider implementation
     */
    /**
     * Get the configured market data provider (default)
     * 
     * @return MarketDataProvider implementation
     */
    public MarketDataProvider getProvider() {
        return getProvider(activeProvider);
    }

    /**
     * Get a specific market data provider by name
     * 
     * @param providerName Name of the provider (zerodha, upstox)
     * @return MarketDataProvider implementation
     */
    public MarketDataProvider getProvider(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            providerName = activeProvider;
        }

        log.debug("Getting market data provider: {}", providerName);

        switch (providerName.toLowerCase()) {
            case "zerodha":
                return applicationContext.getBean("zerodhaMarketDataProvider", MarketDataProvider.class);
            case "upstox":
                return applicationContext.getBean("upstoxMarketDataProvider", MarketDataProvider.class);
            default:
                log.warn("Unknown provider '{}', falling back to Upstox", providerName);
                return applicationContext.getBean("upstoxMarketDataProvider", MarketDataProvider.class);
        }
    }
}
