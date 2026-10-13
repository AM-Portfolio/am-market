package com.am.marketdata.provider.common;

import com.am.marketdata.common.provider.FundamentalDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory and resolver for {@link FundamentalDataProvider}.
 * Dynamically resolves the active fundamentals provider (e.g. UPSTOX) based on configuration,
 * providing zero vendor lock-in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundamentalDataProviderFactory {

    private final List<FundamentalDataProvider> providers;

    @Value("${market.data.fundamentals.active-provider:UPSTOX}")
    private String activeProviderName;

    /**
     * Resolves the configured active fundamental data provider bean.
     *
     * @return active FundamentalDataProvider implementation.
     */
    public FundamentalDataProvider getActiveProvider() {
        Map<String, FundamentalDataProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toUpperCase(),
                        p -> p,
                        (existing, duplicate) -> existing
                ));

        FundamentalDataProvider provider = providerMap.get(activeProviderName.toUpperCase());
        if (provider == null) {
            log.warn("Configured fundamental provider '{}' not found, defaulting to first available", activeProviderName);
            if (!providers.isEmpty()) {
                return providers.get(0);
            }
            throw new IllegalStateException("No FundamentalDataProvider beans registered in Spring context");
        }

        return provider;
    }

    /**
     * Resolves a specific provider by its registered name.
     *
     * @param providerName provider name (e.g. "UPSTOX").
     * @return matching provider or active default.
     */
    public FundamentalDataProvider getProvider(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            return getActiveProvider();
        }

        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName.trim()))
                .findFirst()
                .orElseGet(this::getActiveProvider);
    }
}
