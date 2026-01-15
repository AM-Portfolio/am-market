package com.am.marketdata.provider.resolver;

import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.provider.common.InstrumentContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory for obtaining appropriate SymbolResolver based on provider name.
 * Auto-wires all SymbolResolver implementations and provides lookup by provider
 * name.
 */
@Component
public class SymbolResolverFactory {

    private final AppLogger log = AppLogger.getLogger();
    private final Map<String, SymbolResolver> resolvers;

    @Autowired
    public SymbolResolverFactory(List<SymbolResolver> resolverList) {
        // Auto-wire all SymbolResolver implementations and build provider name map
        this.resolvers = resolverList.stream()
                .collect(Collectors.toMap(
                        resolver -> resolver.getProviderName().toUpperCase(),
                        resolver -> resolver));

        log.info("SymbolResolverFactory",
                "Initialized with " + resolvers.size() + " resolvers: " + resolvers.keySet());
    }

    /**
     * Get resolver for specific provider
     * 
     * @param providerName Provider name (UPSTOX, ZERODHA, etc.)
     * @return SymbolResolver instance
     * @throws IllegalArgumentException if no resolver found for provider
     */
    public SymbolResolver getResolver(String providerName) {
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }

        SymbolResolver resolver = resolvers.get(providerName.toUpperCase());
        if (resolver == null) {
            throw new IllegalArgumentException(
                    "No resolver found for provider: " + providerName +
                            ". Available providers: " + resolvers.keySet());
        }

        return resolver;
    }

    /**
     * Resolve symbols using specified provider
     * 
     * @param symbols      List of trading symbols
     * @param providerName Provider name
     * @return InstrumentContext with resolved instruments
     */
    public InstrumentContext resolve(List<String> symbols, String providerName) {
        SymbolResolver resolver = getResolver(providerName);
        return resolver.resolveContext(symbols);
    }

    /**
     * Check if resolver exists for provider
     * 
     * @param providerName Provider name
     * @return true if resolver exists
     */
    public boolean hasResolver(String providerName) {
        return providerName != null &&
                resolvers.containsKey(providerName.toUpperCase());
    }
}
