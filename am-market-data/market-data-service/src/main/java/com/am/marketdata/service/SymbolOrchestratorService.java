package com.am.marketdata.service;

import com.am.common.investment.service.StockIndicesMarketDataService;
import com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository;
import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.marketdata.provider.upstox.repo.UpstoxInstrumentRepository;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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
    private static final Pattern ISIN_PATTERN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{10}$");

    private final ParserApiClient parserApiClient;
    private final StockIndicesMarketDataService stockIndicesMarketDataService;
    private final StringRedisTemplate stringRedisTemplate;
    private final UpstoxInstrumentRepository upstoxInstrumentRepository;

    /**
     * Optional: loads global index instrument keys from MongoDB so the WebSocket
     * subscribes to them and live ticks populate the global Redis cache.
     * Nullable (fail-open): if the repo is unavailable, Indian streaming continues normally.
     */
    private final GlobalIndexConfigRepository globalIndexConfigRepository;

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

    public SymbolOrchestratorService(
            ParserApiClient parserApiClient,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            @Nullable StringRedisTemplate stringRedisTemplate) {
        this(parserApiClient, stockIndicesMarketDataService, stringRedisTemplate, null, null);
    }

    public SymbolOrchestratorService(
            ParserApiClient parserApiClient,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            @Nullable StringRedisTemplate stringRedisTemplate,
            @Nullable UpstoxInstrumentRepository upstoxInstrumentRepository) {
        this(parserApiClient, stockIndicesMarketDataService, stringRedisTemplate, upstoxInstrumentRepository, null);
    }

    @Autowired
    public SymbolOrchestratorService(
            ParserApiClient parserApiClient,
            StockIndicesMarketDataService stockIndicesMarketDataService,
            @Nullable StringRedisTemplate stringRedisTemplate,
            @Nullable UpstoxInstrumentRepository upstoxInstrumentRepository,
            @Nullable GlobalIndexConfigRepository globalIndexConfigRepository) {
        this.parserApiClient = parserApiClient;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.upstoxInstrumentRepository = upstoxInstrumentRepository;
        this.globalIndexConfigRepository = globalIndexConfigRepository;
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
        Set<String> universe = new HashSet<>(loadOrRefreshBaseSymbols());
        int baseCount = universe.size();
        int portfolioAdded = 0;

        if (includePortfolioActiveSet) {
            Set<String> active = expandActivePortfolioSymbols(loadActivePortfolioSymbols());
            for (String symbol : active) {
                if (universe.add(symbol)) {
                    portfolioAdded++;
                }
            }
        }

        // Merge global index instrument keys so the WebSocket subscribes to them.
        // Ticks matching GLOBAL_* keys are routed by StreamerManager.handleGlobalIndexTick()
        // to market:global-latest:* Redis keys (separate from Indian market:latest-price:*).
        Set<String> globalKeys = getGlobalIndexInstrumentKeys();
        universe.addAll(globalKeys);

        log.info("Symbol aggregation complete. total={}, base≈{}, portfolio_added≈{}, global_indices={}, includePortfolio={}",
                universe.size(), baseCount, portfolioAdded, globalKeys.size(), includePortfolioActiveSet);
        return universe;
    }

    public synchronized void refreshCache() {
        log.info("Refreshing symbol cache...");
        cachedSymbols = null;
        cacheLoadedAt = null;
        findDistinctSymbols();
    }

    /**
     * Loads all enabled global index instrument keys from MongoDB.
     * These Upstox keys (e.g. "GLOBAL_INDEX|DJI") are added to the WebSocket subscription
     * universe so live ticks flow into StreamerManager.handleGlobalIndexTick().
     *
     * <p>Fail-open: returns an empty set if the repository is unavailable,
     * so Indian streaming continues unaffected.
     */
    private Set<String> getGlobalIndexInstrumentKeys() {
        if (globalIndexConfigRepository == null) {
            log.debug("GlobalIndexConfigRepository not available; skipping global index subscription.");
            return Set.of();
        }
        try {
            Set<String> keys = new HashSet<>();
            globalIndexConfigRepository.findAll().forEach(config -> {
                if (config.getInstrumentKey() != null && !config.getInstrumentKey().isBlank()) {
                    keys.add(config.getInstrumentKey());
                }
            });
            log.info("Loaded {} global index instrument keys for WebSocket subscription: {}", keys.size(), keys);
            return keys;
        } catch (Exception e) {
            log.warn("Failed to load global index instrument keys (fail-open): {}", e.getMessage());
            return Set.of();
        }
    }

    private List<String> loadOrRefreshBaseSymbols() {
        if (isCacheValid()) {
            return cachedSymbols;
        }
        log.info("Fetching base symbol universe (defaults ∪ Nifty 500 ∪ ETFs)...");
        List<String> combinedSymbols = new ArrayList<>();
        if (defaultSymbols != null && !defaultSymbols.isEmpty()) {
            combinedSymbols.addAll(List.of(defaultSymbols.split(",")));
        }
        combinedSymbols.addAll(getNifty500Symbols());
        combinedSymbols.addAll(getEtfSymbols());
        cachedSymbols = combinedSymbols.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
        cacheLoadedAt = Instant.now();
        return cachedSymbols;
    }

    /**
     * Redis may contain ISINs. Upstox WS needs trading symbols from upstock_instruments.
     */
    private Set<String> expandActivePortfolioSymbols(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<String> tickers = new HashSet<>();
        List<String> isins = new ArrayList<>();
        for (String member : raw) {
            if (looksLikeIsin(member)) {
                isins.add(member);
            } else {
                tickers.add(member);
            }
        }
        if (isins.isEmpty()) {
            return tickers;
        }
        if (upstoxInstrumentRepository == null) {
            tickers.addAll(isins);
            return tickers;
        }
        try {
            List<UpstoxInstrument> instruments = upstoxInstrumentRepository.findByIsinIn(isins);
            Map<String, UpstoxInstrument> bestByIsin = new HashMap<>();
            if (instruments != null) {
                for (UpstoxInstrument inst : instruments) {
                    if (inst == null || inst.getIsin() == null || inst.getTradingSymbol() == null
                            || inst.getTradingSymbol().isBlank()) {
                        continue;
                    }
                    String isin = inst.getIsin().trim().toUpperCase(Locale.ROOT);
                    UpstoxInstrument existing = bestByIsin.get(isin);
                    if (existing == null || rankInstrument(inst) < rankInstrument(existing)) {
                        bestByIsin.put(isin, inst);
                    }
                }
            }
            for (String isin : isins) {
                UpstoxInstrument inst = bestByIsin.get(isin);
                if (inst != null) {
                    tickers.add(inst.getTradingSymbol().trim().toUpperCase(Locale.ROOT));
                } else {
                    log.warn("Active symbol ISIN {} has no upstock_instruments row; skipped from stream universe",
                            isin);
                }
            }
        } catch (Exception e) {
            log.warn("ISIN expand for active symbols failed (fail-open keep ISINs): {}", e.getMessage());
            tickers.addAll(isins);
        }
        return tickers;
    }

    private static boolean looksLikeIsin(String value) {
        return value != null && ISIN_PATTERN.matcher(value).matches();
    }

    private static int rankInstrument(UpstoxInstrument inst) {
        String exchange = inst.getExchange() != null ? inst.getExchange().trim().toUpperCase(Locale.ROOT) : "";
        String type = inst.getInstrumentType() != null ? inst.getInstrumentType().trim().toUpperCase(Locale.ROOT) : "";
        if (exchange.contains("NFO") || exchange.contains("BFO") || exchange.contains("MCX")
                || type.contains("FUT") || type.contains("OPT")) {
            return 100;
        }
        if (exchange.startsWith("NSE") && (type.contains("EQ") || type.contains("GB") || type.isEmpty())) {
            return 0;
        }
        if (exchange.startsWith("NSE")) {
            return 1;
        }
        if (exchange.startsWith("BSE")) {
            return 2;
        }
        return 3;
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
