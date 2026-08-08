package com.am.marketdata.service;

import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.marketdata.service.client.ParserApiClient;
import com.am.common.investment.model.stockindice.StockData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the symbol universe for schedulers and streamer:
 * defaults ∪ Nifty 500 ∪ ETFs ∪ (optional) Redis active portfolio symbols.
 *
 * <p>Portfolio symbols are published by am-portfolio into {@code market:active-symbols}.
 * Redis failures are fail-open: the base Nifty/ETF list still works.
 */
@Slf4j
@Service
public class SymbolOrchestratorService {

    public static final String DEFAULT_ACTIVE_SET_KEY = "market:active-symbols";

    private final ParserApiClient parserApiClient;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${scheduler.symbols.default:RELIANCE}")
    private String defaultSymbols;

    @Value("${scheduler.symbols.include-portfolio-active-set:true}")
    private boolean includePortfolioActiveSet;

    @Value("${scheduler.symbols.active-set-redis-key:" + DEFAULT_ACTIVE_SET_KEY + "}")
    private String activeSetRedisKey;

    @Value("${scheduler.symbols.cache-ttl-seconds:900}")
    private long cacheTtlSeconds;

    private List<String> cachedSymbols = null;
    private Instant cacheLoadedAt = null;

    @Autowired
    public SymbolOrchestratorService(
            ParserApiClient parserApiClient,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            @Nullable StringRedisTemplate stringRedisTemplate) {
        this.parserApiClient = parserApiClient;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public List<String> getNifty500Symbols() {
        return getIndexSymbols(Set.of("NIFTY 500"));
    }

    public List<String> getEtfSymbols() {
        return parserApiClient.getAllEtfSymbols();
    }

    public List<String> getIndexSymbols(Set<String> indicesToCheck) {
        Set<String> constituents = new HashSet<>();

        for (String index : indicesToCheck) {
            try {
                var indexData = stockIndicesMarketDataService.findByIndexSymbol(index);
                if (indexData != null && indexData.getData() != null) {
                    constituents.addAll(indexData.getData().stream()
                            .map(StockData::getSymbol)
                            .collect(Collectors.toList()));
                }
            } catch (Exception e) {
                log.error("Failed to fetch constituents for index: {}", index, e);
            }
        }
        return constituents.stream().toList();
    }

    public synchronized Set<String> findDistinctSymbols() {
        if (isCacheValid()) {
            log.info("Returning cached symbols. Count: {}", cachedSymbols.size());
            return new HashSet<>(cachedSymbols);
        }

        log.info("Fetching distinct symbols from all sources...");
        List<String> combinedSymbols = new ArrayList<>();
        int baseCount;
        int portfolioAdded = 0;

        if (defaultSymbols != null && !defaultSymbols.isEmpty()) {
            combinedSymbols.addAll(List.of(defaultSymbols.split(",")));
        }

        combinedSymbols.addAll(getNifty500Symbols());
        combinedSymbols.addAll(getEtfSymbols());

        baseCount = (int) combinedSymbols.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .count();

        if (includePortfolioActiveSet) {
            Set<String> active = loadActivePortfolioSymbols();
            if (!active.isEmpty()) {
                Set<String> before = combinedSymbols.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(s -> s.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                combinedSymbols.addAll(active);
                portfolioAdded = (int) active.stream().filter(s -> !before.contains(s)).count();
            }
        }

        cachedSymbols = combinedSymbols.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
        cacheLoadedAt = Instant.now();

        log.info("Symbol aggregation complete. total={}, base≈{}, portfolio_added≈{}, includePortfolio={}",
                cachedSymbols.size(), baseCount, portfolioAdded, includePortfolioActiveSet);
        return new HashSet<>(cachedSymbols);
    }

    public synchronized void refreshCache() {
        log.info("Refreshing symbol cache...");
        cachedSymbols = null;
        cacheLoadedAt = null;
        findDistinctSymbols();
    }

    /** Package-visible for tests. */
    void setIncludePortfolioActiveSet(boolean includePortfolioActiveSet) {
        this.includePortfolioActiveSet = includePortfolioActiveSet;
    }

    void setActiveSetRedisKey(String activeSetRedisKey) {
        this.activeSetRedisKey = activeSetRedisKey;
    }

    void setDefaultSymbols(String defaultSymbols) {
        this.defaultSymbols = defaultSymbols;
    }

    void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    private boolean isCacheValid() {
        if (cachedSymbols == null || cachedSymbols.isEmpty() || cacheLoadedAt == null) {
            return false;
        }
        if (cacheTtlSeconds <= 0) {
            return true;
        }
        return Instant.now().isBefore(cacheLoadedAt.plusSeconds(cacheTtlSeconds));
    }

    private Set<String> loadActivePortfolioSymbols() {
        if (stringRedisTemplate == null) {
            log.warn("StringRedisTemplate unavailable; skipping portfolio active-symbol set");
            return Set.of();
        }
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(activeSetRedisKey);
            if (members == null || members.isEmpty()) {
                log.info("No members in Redis active set key={}", activeSetRedisKey);
                return Set.of();
            }
            Set<String> cleaned = members.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            log.info("Loaded {} portfolio active symbols from Redis key={}", cleaned.size(), activeSetRedisKey);
            return cleaned;
        } catch (Exception e) {
            log.warn("Failed to read portfolio active symbols from Redis (fail-open): {}", e.getMessage());
            return Set.of();
        }
    }
}
