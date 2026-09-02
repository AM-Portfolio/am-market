package com.am.marketdata.service.fundamental;

import com.am.marketdata.common.model.fundamental.*;
import com.am.marketdata.common.provider.FundamentalDataProvider;
import com.am.marketdata.provider.common.FundamentalDataProviderFactory;
import com.am.marketdata.service.MarketDataCacheService;
import com.am.marketdata.service.model.security.SecurityDocument;
import com.am.marketdata.service.repo.FundamentalDataRepository;
import com.am.marketdata.service.repo.SecurityRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrator service managing the complete fundamental data workflow:
 * User -> API -> Redis Cache -> MongoDB -> (Asynchronous Lazy Hydration from Provider).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundamentalQueryService {

    private final FundamentalDataRepository fundamentalDataRepository;
    private final SecurityRepository securityRepository;
    private final FundamentalDataProviderFactory providerFactory;
    private final FundamentalCalculationEngine calculationEngine;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    // Local in-memory lock map to prevent redundant concurrent initial fetches for the same ISIN
    private final Map<String, Boolean> inFlightHydrations = new ConcurrentHashMap<>();

    /**
     * Resolves a stock symbol or ISIN string to a validated ISIN code.
     * Supports case-insensitive trading symbols (e.g. "tcs", "TCS", "Itc", "HDFCBANK")
     * as well as standard ISIN codes (e.g. "INE154A01025").
     */
    public String resolveIsin(String symbolOrIsin) {
        if (symbolOrIsin == null || symbolOrIsin.trim().isEmpty()) {
            return null;
        }

        String cleaned = symbolOrIsin.trim();
        if (cleaned.contains("|")) {
            cleaned = cleaned.substring(cleaned.indexOf("|") + 1).trim();
        }
        if (cleaned.contains(":")) {
            cleaned = cleaned.substring(cleaned.indexOf(":") + 1).trim();
        }

        // Direct ISIN detection: 12-char starting with IN / 2-letter country code
        if (cleaned.toUpperCase().matches("^[A-Z]{2}[A-Z0-9]{10}$")) {
            return cleaned.toUpperCase();
        }

        // Generate candidate variations for robust fuzzy matching (handles spaces, hyphens, underscores, dots)
        List<String> candidates = new ArrayList<>();
        candidates.add(cleaned);

        String compact = cleaned.replaceAll("[\\s\\-_.]+", "");
        if (!compact.isEmpty() && !candidates.contains(compact)) {
            candidates.add(compact);
        }

        String hyphenated = cleaned.replaceAll("[\\s_.]+", "-");
        if (!hyphenated.isEmpty() && !candidates.contains(hyphenated)) {
            candidates.add(hyphenated);
        }

        String underscored = cleaned.replaceAll("[\\s\\-.]+", "_");
        if (!underscored.isEmpty() && !candidates.contains(underscored)) {
            candidates.add(underscored);
        }

        if (cleaned.toUpperCase().contains(" AND ")) {
            String ampersand = cleaned.replaceAll("(?i)\\s+and\\s+", "&");
            if (!candidates.contains(ampersand)) candidates.add(ampersand);
        } else if (cleaned.contains("&")) {
            String andVariant = cleaned.replaceAll("&", " AND ");
            if (!candidates.contains(andVariant)) candidates.add(andVariant);
        }

        // Try candidate symbol lookups across collections
        for (String candidate : candidates) {
            String isin = queryIsinByExactSymbol(candidate);
            if (isin != null) {
                return isin;
            }
        }

        // Fallback 1: Match by Company Name prefix/exact in fundamental_analysis & securities
        String isinByCompany = queryIsinByCompanyName(cleaned);
        if (isinByCompany != null) {
            return isinByCompany;
        }

        // Fallback 2: SecurityRepository text search
        for (String candidate : candidates) {
            try {
                List<SecurityDocument> searchResults = securityRepository.search(candidate);
                if (searchResults != null && !searchResults.isEmpty()) {
                    SecurityDocument sec = searchResults.get(0);
                    if (sec.getKey() != null && sec.getKey().getIsin() != null) {
                        return sec.getKey().getIsin().toUpperCase();
                    }
                }
            } catch (Exception e) {
                log.debug("Error in search lookup for {}: {}", candidate, e.getMessage());
            }
        }

        return null;
    }

    private String queryIsinByExactSymbol(String symbolCandidate) {
        // 1. Direct MongoTemplate query on 'securities' collection
        try {
            org.bson.Document query = new org.bson.Document("key.symbol", java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(symbolCandidate) + "$", java.util.regex.Pattern.CASE_INSENSITIVE));
            org.bson.Document found = mongoTemplate.getCollection("securities").find(query).first();
            if (found != null) {
                org.bson.Document keyDoc = found.get("key", org.bson.Document.class);
                if (keyDoc != null && keyDoc.getString("isin") != null) {
                    String isin = keyDoc.getString("isin").toUpperCase();
                    log.info("Resolved symbolCandidate={} to isin={} via securities collection", symbolCandidate, isin);
                    return isin;
                }
            }
        } catch (Exception e) {
            log.debug("Direct MongoDB symbol lookup note for {}: {}", symbolCandidate, e.getMessage());
        }

        // 2. Check 'stock_indices_market_data' collection
        try {
            org.bson.Document query = new org.bson.Document("data.symbol", java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(symbolCandidate) + "$", java.util.regex.Pattern.CASE_INSENSITIVE));
            org.bson.Document found = mongoTemplate.getCollection("stock_indices_market_data").find(query).first();
            if (found != null) {
                List<?> dataList = found.get("data", List.class);
                if (dataList != null) {
                    for (Object item : dataList) {
                        if (item instanceof org.bson.Document doc) {
                            String sym = doc.getString("symbol");
                            String isin = doc.getString("isin");
                            if (sym != null && isin != null && sym.equalsIgnoreCase(symbolCandidate)) {
                                log.info("Resolved symbolCandidate={} to isin={} via stock_indices_market_data", symbolCandidate, isin.toUpperCase());
                                return isin.toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("stock_indices_market_data symbol lookup note for {}: {}", symbolCandidate, e.getMessage());
        }

        // 3. Check 'fundamental_analysis' collection
        try {
            org.bson.Document query = new org.bson.Document("symbol", java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(symbolCandidate) + "$", java.util.regex.Pattern.CASE_INSENSITIVE));
            org.bson.Document found = mongoTemplate.getCollection("fundamental_analysis").find(query).first();
            if (found != null && found.getString("isin") != null) {
                String isin = found.getString("isin").toUpperCase();
                log.info("Resolved symbolCandidate={} to isin={} via fundamental_analysis collection", symbolCandidate, isin);
                return isin;
            }
        } catch (Exception e) {
            log.debug("Direct fundamental_analysis lookup note for {}: {}", symbolCandidate, e.getMessage());
        }

        return null;
    }

    private String queryIsinByCompanyName(String nameCandidate) {
        if (nameCandidate == null || nameCandidate.trim().length() < 3) {
            return null;
        }
        try {
            org.bson.Document query = new org.bson.Document("companyName", java.util.regex.Pattern.compile("^" + java.util.regex.Pattern.quote(nameCandidate.trim()) + ".*", java.util.regex.Pattern.CASE_INSENSITIVE));
            org.bson.Document found = mongoTemplate.getCollection("fundamental_analysis").find(query).first();
            if (found != null && found.getString("isin") != null) {
                String isin = found.getString("isin").toUpperCase();
                log.info("Resolved nameCandidate='{}' to isin={} via companyName in fundamental_analysis", nameCandidate, isin);
                return isin;
            }
        } catch (Exception e) {
            log.debug("Company name lookup note for {}: {}", nameCandidate, e.getMessage());
        }

        return null;
    }

    /**
     * Retrieves fundamental analysis data for a stock by trading symbol or ISIN.
     */
    public Optional<FundamentalData> getFundamentalsBySymbolOrIsin(String symbolOrIsin) {
        String isin = resolveIsin(symbolOrIsin);
        if (isin == null) {
            return Optional.empty();
        }
        return getFundamentalsByIsin(isin);
    }

    /**
     * Retrieves fundamental analysis data for a stock by ISIN directly from MongoDB.
     * Keeps Redis clean and memory-efficient by avoiding caching large multi-year statement datasets.
     *
     * @param isin International Securities Identification Number.
     * @return populated {@link FundamentalData} or empty if unresolvable.
     */
    public Optional<FundamentalData> getFundamentalsByIsin(String isin) {
        if (isin == null || isin.trim().isEmpty()) {
            return Optional.empty();
        }
        String cleanIsin = resolveIsin(isin);
        if (cleanIsin == null) {
            cleanIsin = isin.trim().toUpperCase();
        }

        // 1. Direct MongoDB Query (Fast B-Tree Indexed Lookup on 'isin')
        Optional<FundamentalData> mongoDataOpt = fundamentalDataRepository.findFirstByIsin(cleanIsin);
        if (mongoDataOpt.isPresent()) {
            log.debug("MongoDB HIT for fundamentals isin={}", cleanIsin);
            return mongoDataOpt;
        }

        // 2. Lazy Initial Seeding (Synchronously fetch Profile + Key Ratios; queue the rest asynchronously)
        log.info("Stock fundamentals not found in DB, initiating lazy seed for isin={}", cleanIsin);
        FundamentalData seededData = seedInitialEssentials(cleanIsin);
        if (seededData != null) {
            // Trigger asynchronous hydration for heavy statement reports
            triggerAsyncFullHydration(cleanIsin);
            return Optional.of(seededData);
        }

        return Optional.empty();
    }

    /**
     * Directly retrieves fundamental data by ISIN without triggering lazy seeding.
     */
    public Optional<FundamentalData> getExistingFundamentalsByIsin(String isin) {
        if (isin == null || isin.trim().isEmpty()) {
            return Optional.empty();
        }
        return fundamentalDataRepository.findFirstByIsin(isin.trim().toUpperCase());
    }

    /**
     * Resolves security metadata by ISIN from MongoDB.
     */
    public Optional<SecurityDocument> getSecurityByIsin(String isin) {
        if (isin == null || isin.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(securityRepository.findByIsin(isin.trim().toUpperCase()));
        } catch (Exception e) {
            log.debug("Failed to lookup security document for isin={}: {}", isin, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Synchronously fetches essentials (Company Profile, Key Ratios) so the UI responds immediately.
     */
    public FundamentalData seedInitialEssentials(String isin) {
        FundamentalDataProvider provider = providerFactory.getActiveProvider();
        if (provider == null) {
            log.error("No active FundamentalDataProvider configured");
            return null;
        }

        // Resolve symbol & company name from securities collection
        String symbol = isin;
        String companyName = isin;
        String instrumentKey = "NSE_EQ|" + isin;
        try {
            org.bson.Document query = new org.bson.Document("key.isin", isin);
            org.bson.Document secDoc = mongoTemplate.getCollection("securities").find(query).first();
            if (secDoc != null) {
                org.bson.Document keyDoc = secDoc.get("key", org.bson.Document.class);
                if (keyDoc != null && keyDoc.getString("symbol") != null) {
                    symbol = keyDoc.getString("symbol");
                }
                org.bson.Document metaDoc = secDoc.get("metadata", org.bson.Document.class);
                if (metaDoc != null && metaDoc.getString("company_name") != null) {
                    companyName = metaDoc.getString("company_name");
                }
            } else {
                // Fallback to stock_indices_market_data
                org.bson.Document idxQuery = new org.bson.Document("data.isin", isin);
                org.bson.Document idxDoc = mongoTemplate.getCollection("stock_indices_market_data").find(idxQuery).first();
                if (idxDoc != null) {
                    List<?> dataList = idxDoc.get("data", List.class);
                    if (dataList != null) {
                        for (Object item : dataList) {
                            if (item instanceof org.bson.Document d && isin.equalsIgnoreCase(d.getString("isin"))) {
                                if (d.getString("symbol") != null) symbol = d.getString("symbol");
                                if (d.getString("companyName") != null) companyName = d.getString("companyName");
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Security resolution note for isin={}: {}", isin, e.getMessage());
        }

        CompanyProfile profile = provider.getCompanyProfile(isin);
        KeyRatios ratios = provider.getKeyRatios(isin);

        Map<String, Instant> timestamps = new HashMap<>();
        Instant now = Instant.now();
        timestamps.put("profile", now);
        timestamps.put("keyRatios", now);

        FundamentalData fundamentalData = FundamentalData.builder()
                .isin(isin)
                .symbol(symbol)
                .companyName(companyName)
                .instrumentKey(instrumentKey)
                .providerSource(provider.getProviderName())
                .companyProfile(profile)
                .keyRatios(ratios)
                .sectionLastUpdated(timestamps)
                .createdAt(now)
                .updatedAt(now)
                .lastUpdated(now)
                .build();

        try {
            return fundamentalDataRepository.save(fundamentalData);
        } catch (Exception e) {
            log.error("Failed to save initial seeded fundamental data for isin={}: {}", isin, e.getMessage());
            return fundamentalData;
        }
    }

    // [Single-Flight In-Flight Request Registry]
    // Coalesces concurrent requests for the exact same stock section into a single shared Future
    private final Map<String, CompletableFuture<?>> inFlightSectionFutures = new ConcurrentHashMap<>();

    /**
     * [Single-Flight Request Coalescing Engine]
     * Ensures that if 100 concurrent users request a missing section (e.g. WIPRO:shareholding),
     * exactly ONE thread makes the upstream Upstox API call and saves to MongoDB. All other 99 threads
     * await the same Future and receive the result simultaneously without wasting API quota.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrFetchSection(String isin, String sectionKey, java.util.function.Supplier<T> fetcher, java.util.function.Consumer<T> saver) {
        String lockKey = isin.toUpperCase() + ":" + sectionKey;

        CompletableFuture<T> future = (CompletableFuture<T>) inFlightSectionFutures.computeIfAbsent(lockKey, k -> CompletableFuture.supplyAsync(() -> {
            try {
                T data = fetcher.get();
                if (data != null) {
                    saver.accept(data);
                }
                return data;
            } finally {
                inFlightSectionFutures.remove(lockKey);
            }
        }));

        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            inFlightSectionFutures.remove(lockKey);
            log.error("SingleFlight fetch encountered exception for key={}: {}", lockKey, e.getMessage());
            try {
                T fallbackData = fetcher.get();
                if (fallbackData != null) {
                    saver.accept(fallbackData);
                }
                return fallbackData;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public List<ShareholdingQuarterEntry> hydrateShareholding(String isin) {
        return getOrFetchSection(isin, "shareholding", () -> {
            FundamentalDataProvider provider = providerFactory.getActiveProvider();
            return provider != null ? provider.getShareHoldings(isin) : Collections.emptyList();
        }, list -> {
            if (list != null && !list.isEmpty()) {
                updateDocumentSection(isin, "shareholding", doc -> doc.setShareholdings(list));
            }
        });
    }

    public List<IncomeStatementEntry> hydrateIncomeStatements(String isin) {
        return getOrFetchSection(isin, "incomeStatement", () -> {
            FundamentalDataProvider provider = providerFactory.getActiveProvider();
            return provider != null ? provider.getIncomeStatement(isin, true, "yearly") : Collections.emptyList();
        }, list -> {
            if (list != null && !list.isEmpty()) {
                updateDocumentSection(isin, "incomeStatement", doc -> doc.setIncomeStatements(list));
            }
        });
    }

    public List<BalanceSheetEntry> hydrateBalanceSheets(String isin) {
        return getOrFetchSection(isin, "balanceSheet", () -> {
            FundamentalDataProvider provider = providerFactory.getActiveProvider();
            return provider != null ? provider.getBalanceSheet(isin, true) : Collections.emptyList();
        }, list -> {
            if (list != null && !list.isEmpty()) {
                updateDocumentSection(isin, "balanceSheet", doc -> doc.setBalanceSheets(list));
            }
        });
    }

    public List<CashFlowEntry> hydrateCashFlows(String isin) {
        return getOrFetchSection(isin, "cashFlow", () -> {
            FundamentalDataProvider provider = providerFactory.getActiveProvider();
            return provider != null ? provider.getCashFlow(isin, true) : Collections.emptyList();
        }, list -> {
            if (list != null && !list.isEmpty()) {
                updateDocumentSection(isin, "cashFlow", doc -> doc.setCashFlows(list));
            }
        });
    }

    public List<CorporateActionEntry> hydrateCorporateActions(String isin) {
        return getOrFetchSection(isin, "corporateActions", () -> {
            FundamentalDataProvider provider = providerFactory.getActiveProvider();
            return provider != null ? provider.getCorporateActions(isin) : Collections.emptyList();
        }, list -> {
            if (list != null && !list.isEmpty()) {
                updateDocumentSection(isin, "corporateActions", doc -> doc.setCorporateActions(list));
            }
        });
    }

    public List<CompetitorPeer> hydratePeers(String isin) {
        return getOrFetchSection(isin, "peers", () -> {
            FundamentalDataProvider provider = providerFactory.getActiveProvider();
            return provider != null ? provider.getCompetitors(isin) : Collections.emptyList();
        }, list -> {
            if (list != null && !list.isEmpty()) {
                updateDocumentSection(isin, "competitors", doc -> doc.setPeers(list));
            }
        });
    }

    private void updateDocumentSection(String isin, String sectionName, java.util.function.Consumer<FundamentalData> updater) {
        try {
            Optional<FundamentalData> docOpt = fundamentalDataRepository.findByIsin(isin);
            Instant now = Instant.now();
            FundamentalData doc = docOpt.orElseGet(() -> FundamentalData.builder()
                    .isin(isin)
                    .createdAt(now)
                    .build());
            updater.accept(doc);
            if (doc.getCreatedAt() == null) doc.setCreatedAt(now);
            if (doc.getSectionLastUpdated() == null) doc.setSectionLastUpdated(new HashMap<>());
            doc.getSectionLastUpdated().put(sectionName, now);
            doc.setUpdatedAt(now);
            doc.setLastUpdated(now);
            fundamentalDataRepository.save(doc);
            log.info("Persisted on-demand section '{}' to MongoDB for isin={}", sectionName, isin);
        } catch (Exception e) {
            log.warn("Failed to persist section '{}' for isin={}: {}", sectionName, isin, e.getMessage());
        }
    }

    /**
     * Asynchronously hydrates remaining financial statements, shareholdings, corporate actions, and peers.
     * Each section is isolated in its own try-catch so a failure in one section never aborts the rest.
     */
    @Async
    public void triggerAsyncFullHydration(String isin) {
        if (inFlightHydrations.putIfAbsent(isin, Boolean.TRUE) != null) {
            return; // Hydration already running
        }

        try {
            log.info("Starting isolated async full hydration for isin={}", isin);
            FundamentalDataProvider provider = providerFactory.getActiveProvider();
            if (provider == null) return;

            List<IncomeStatementEntry> income = null;
            try { income = provider.getIncomeStatement(isin, true, "yearly"); } catch (Exception e) { log.debug("Async income note for {}: {}", isin, e.getMessage()); }

            List<BalanceSheetEntry> balance = null;
            try { balance = provider.getBalanceSheet(isin, true); } catch (Exception e) { log.debug("Async balance note for {}: {}", isin, e.getMessage()); }

            List<CashFlowEntry> cashFlow = null;
            try { cashFlow = provider.getCashFlow(isin, true); } catch (Exception e) { log.debug("Async cashflow note for {}: {}", isin, e.getMessage()); }

            List<ShareholdingQuarterEntry> shareholding = null;
            try { shareholding = provider.getShareHoldings(isin); } catch (Exception e) { log.debug("Async shareholding note for {}: {}", isin, e.getMessage()); }

            List<CorporateActionEntry> actions = null;
            try { actions = provider.getCorporateActions(isin); } catch (Exception e) { log.debug("Async actions note for {}: {}", isin, e.getMessage()); }

            List<CompetitorPeer> peers = null;
            try { peers = provider.getCompetitors(isin); } catch (Exception e) { log.debug("Async peers note for {}: {}", isin, e.getMessage()); }

            // Compute analytics metrics
            FundamentalAnalytics analytics = null;
            try {
                analytics = calculationEngine.computeAnalytics(
                        income != null ? income : Collections.emptyList(),
                        balance != null ? balance : Collections.emptyList(),
                        cashFlow != null ? cashFlow : Collections.emptyList(),
                        null, null, null, null, null
                );
            } catch (Exception e) {
                log.debug("Async analytics computation note for {}: {}", isin, e.getMessage());
            }

            Optional<FundamentalData> existingOpt = fundamentalDataRepository.findByIsin(isin);
            Instant now = Instant.now();
            FundamentalData doc = existingOpt.orElseGet(() -> FundamentalData.builder()
                    .isin(isin)
                    .createdAt(now)
                    .build());

            if (income != null && !income.isEmpty()) doc.setIncomeStatements(income);
            if (balance != null && !balance.isEmpty()) doc.setBalanceSheets(balance);
            if (cashFlow != null && !cashFlow.isEmpty()) doc.setCashFlows(cashFlow);
            if (shareholding != null && !shareholding.isEmpty()) doc.setShareholdings(shareholding);
            if (actions != null && !actions.isEmpty()) doc.setCorporateActions(actions);
            if (peers != null && !peers.isEmpty()) doc.setPeers(peers);
            if (analytics != null) doc.setAnalytics(analytics);

            if (doc.getCreatedAt() == null) {
                doc.setCreatedAt(now);
            }
            if (doc.getSectionLastUpdated() == null) {
                doc.setSectionLastUpdated(new HashMap<>());
            }
            if (income != null) doc.getSectionLastUpdated().put("incomeStatement", now);
            if (balance != null) doc.getSectionLastUpdated().put("balanceSheet", now);
            if (cashFlow != null) doc.getSectionLastUpdated().put("cashFlow", now);
            if (shareholding != null) doc.getSectionLastUpdated().put("shareholding", now);
            if (actions != null) doc.getSectionLastUpdated().put("corporateActions", now);
            if (peers != null) doc.getSectionLastUpdated().put("competitors", now);
            doc.setUpdatedAt(now);
            doc.setLastUpdated(now);

            fundamentalDataRepository.save(doc);
            log.info("Successfully completed isolated full hydration for isin={}", isin);
        } catch (Exception e) {
            log.error("Error during async full hydration for isin={}: {}", isin, e.getMessage(), e);
        } finally {
            inFlightHydrations.remove(isin);
        }
    }
}
