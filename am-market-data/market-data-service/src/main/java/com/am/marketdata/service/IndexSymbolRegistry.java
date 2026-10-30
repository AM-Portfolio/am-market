package com.am.marketdata.service;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.service.StockIndicesMarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Resolves index identity without keeping an index universe in Java code.
 *
 * <p>The configured NSE registry is loaded from {@code nseindices.yml}; symbols
 * not present there are resolved through the existing Mongo-backed index metadata
 * service. This lets newly added indexes work without a code release.</p>
 */
@Slf4j
@Component
public class IndexSymbolRegistry {

    private static final List<String> NSE_GROUPS = List.of(
            "broad-market-indices", "sector-indices", "index-aliases");

    private final StockIndicesMarketDataService metadataService;
    private final Map<String, Boolean> classificationCache = new ConcurrentHashMap<>();
    private final Set<String> configuredIndexSymbols;

    public IndexSymbolRegistry(StockIndicesMarketDataService metadataService) {
        this.metadataService = metadataService;
        this.configuredIndexSymbols = loadConfiguredIndexSymbols();
        configuredIndexSymbols.forEach(symbol -> classificationCache.put(symbol, true));
    }

    /**
     * Resolves the supplied symbols to the normalized names known to be indexes.
     * Explicit index keys are handled by the caller because global indexes use a
     * separate persistence path.
     */
    public Set<String> resolveKnownIndexNames(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalizedSymbols = symbols.stream()
                .map(this::normalizeSymbol)
                .filter(symbol -> !symbol.isBlank())
                .collect(Collectors.toSet());
        Set<String> unresolvedSymbols = normalizedSymbols.stream()
                .filter(symbol -> !classificationCache.containsKey(symbol))
                .collect(Collectors.toSet());

        if (!unresolvedSymbols.isEmpty()) {
            try {
                List<StockIndicesMarketData> knownIndices = metadataService
                        .findByIndexSymbols(unresolvedSymbols);
                Set<String> resolvedIndexSymbols = knownIndices == null
                        ? Collections.emptySet()
                        : knownIndices.stream()
                                .map(StockIndicesMarketData::getIndexSymbol)
                                .filter(symbol -> symbol != null && !symbol.isBlank())
                                .map(this::normalizeSymbol)
                                .collect(Collectors.toSet());

                unresolvedSymbols.forEach(symbol ->
                        classificationCache.put(symbol, resolvedIndexSymbols.contains(symbol)));
            } catch (Exception e) {
                // Do not mark symbols as equities when metadata is temporarily unavailable.
                // Explicit prefixes and YAML-configured names remain authoritative.
                log.warn("Unable to refresh index metadata for {} symbols: {}",
                        unresolvedSymbols.size(), e.getMessage());
            }
        }

        return normalizedSymbols.stream()
                .filter(symbol -> Boolean.TRUE.equals(classificationCache.get(symbol)))
                .collect(Collectors.toSet());
    }

    public boolean isExplicitIndexKey(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("NSE_INDEX|") || normalized.startsWith("BSE_INDEX|")
                || normalized.startsWith("NSE_INDEX:") || normalized.startsWith("BSE_INDEX:");
    }

    public String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null) {
            return "";
        }

        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        for (String prefix : List.of("GLOBAL_INDEX|", "GLOBAL_INDEX:", "NSE_INDEX|", "NSE_INDEX:",
                "BSE_INDEX|", "BSE_INDEX:", "NSE_EQ|", "BSE_EQ|", "NSE_EQ:", "BSE_EQ:",
                "NSE:", "BSE:")) {
            if (symbol.startsWith(prefix)) {
                return symbol.substring(prefix.length()).trim();
            }
        }
        return symbol;
    }

    Set<String> getConfiguredIndexSymbols() {
        return configuredIndexSymbols;
    }

    private Set<String> loadConfiguredIndexSymbols() {
        ClassPathResource resource = new ClassPathResource("nseindices.yml");
        if (!resource.exists()) {
            log.warn("nseindices.yml not found on classpath; index identity will use Mongo metadata");
            return Collections.emptySet();
        }

        try (InputStream input = resource.getInputStream()) {
            Object loaded = new Yaml().load(input);
            if (!(loaded instanceof Map<?, ?> root) || !(root.get("nse") instanceof Map<?, ?> nse)) {
                log.warn("nseindices.yml has an unexpected structure");
                return Collections.emptySet();
            }

            Set<String> symbols = new HashSet<>();
            for (String group : NSE_GROUPS) {
                Object values = nse.get(group);
                if (values instanceof List<?> list) {
                    list.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(this::normalizeSymbol)
                            .filter(symbol -> !symbol.isBlank())
                            .forEach(symbols::add);
                }
            }

            Set<String> result = Collections.unmodifiableSet(symbols);
            log.info("Loaded {} configured NSE index symbols from nseindices.yml", result.size());
            return result;
        } catch (Exception e) {
            log.warn("Unable to load configured index symbols from nseindices.yml: {}", e.getMessage());
            return Collections.emptySet();
        }
    }
}
