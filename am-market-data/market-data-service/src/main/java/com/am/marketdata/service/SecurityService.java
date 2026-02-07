package com.am.marketdata.service;

import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.service.dto.SecuritySearchRequest;
import com.am.marketdata.service.model.security.SecurityDocument;
import com.am.marketdata.service.repo.SecurityRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

@Service
public class SecurityService {

    private final AppLogger log = AppLogger.getLogger();
    private final SecurityRepository securityRepository;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.am.common.investment.service.StockIndicesMarketDataService stockIndicesMarketDataService;

    public SecurityService(SecurityRepository securityRepository, MongoTemplate mongoTemplate,
            RedisTemplate<String, Object> redisTemplate,
            com.am.common.investment.service.StockIndicesMarketDataService stockIndicesMarketDataService) {
        this.securityRepository = securityRepository;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private List<ManualSecurityUpdate> manualSecurityUpdates = new ArrayList<>();

    @PostConstruct
    public void loadManualUpdates() {
        try {
            ClassPathResource resource = new ClassPathResource("manual_isin_updates.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    manualSecurityUpdates = objectMapper.readValue(inputStream,
                            new TypeReference<List<ManualSecurityUpdate>>() {
                            });
                    log.info("loadManualUpdates",
                            "Loaded " + manualSecurityUpdates.size() + " manual security updates.");
                }
            } else {
                log.warn("loadManualUpdates", "manual_isin_updates.json not found in classpath.");
            }
        } catch (Exception e) {
            log.error("loadManualUpdates", "Error loading manual_isin_updates.json", e);
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ManualSecurityUpdate {
        @com.fasterxml.jackson.annotation.JsonProperty("company_name")
        private String companyName;
        private String isin;
        @com.fasterxml.jackson.annotation.JsonProperty("market_cap_category")
        private String marketCapCategory;
        @com.fasterxml.jackson.annotation.JsonProperty("market_cap_value")
        private Long marketCapValue;
        @com.fasterxml.jackson.annotation.JsonProperty("market_cap_type")
        private String marketCapType;
    }

    private static final String CACHE_PREFIX = "security:metadata:";
    private static final long CACHE_TTL_DAYS = 7;

    /**
     * Find securities by a list of symbols with caching (Granular per symbol)
     */
    public List<SecurityDocument> findBySymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Check Redis for all symbols
        List<String> keys = symbols.stream()
                .map(s -> CACHE_PREFIX + s.toUpperCase())
                .collect(Collectors.toList());

        List<Object> cachedDocs;
        try {
            cachedDocs = redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            log.error("findBySymbols", "Error fetching from Redis", e);
            cachedDocs = Collections.nCopies(symbols.size(), null);
        }

        List<SecurityDocument> results = new ArrayList<>();
        List<String> missingSymbols = new ArrayList<>();

        // 2. Process cache results
        for (int i = 0; i < symbols.size(); i++) {
            Object obj = (cachedDocs != null && cachedDocs.size() > i) ? cachedDocs.get(i) : null;
            if (obj != null) {
                try {
                    // Handle map/linkedhashmap vs actual object (depending on Redis serializer)
                    if (obj instanceof SecurityDocument) {
                        results.add((SecurityDocument) obj);
                    } else {
                        SecurityDocument doc = objectMapper.convertValue(obj, SecurityDocument.class);
                        results.add(doc);
                    }
                } catch (Exception e) {
                    log.error("findBySymbols", "Error deserializing cached security for " + symbols.get(i), e);
                    missingSymbols.add(symbols.get(i));
                }
            } else {
                missingSymbols.add(symbols.get(i));
            }
        }

        // 3. Fetch missing from DB
        if (!missingSymbols.isEmpty()) {
            List<SecurityDocument> dbDocs = securityRepository.findBySymbolIn(missingSymbols);

            // Map back to handle duplicates or ordering if needed
            Map<String, SecurityDocument> dbMap = new HashMap<>();
            dbDocs.forEach(d -> {
                if (d.getKey() != null && d.getKey().getSymbol() != null) {
                    dbMap.put(d.getKey().getSymbol(), d);
                }
            });

            // Add found DB docs to results
            results.addAll(dbDocs);

            // 4. Update Cache for found items
            Map<String, Object> cacheUpdates = new HashMap<>();
            for (SecurityDocument doc : dbDocs) {
                if (doc.getKey() != null && doc.getKey().getSymbol() != null) {
                    String key = CACHE_PREFIX + doc.getKey().getSymbol().toUpperCase();
                    cacheUpdates.put(key, doc);
                }
            }

            if (!cacheUpdates.isEmpty()) {
                try {
                    redisTemplate.opsForValue().multiSet(cacheUpdates);
                    // Use simpler logic for expiration if multiSet doesn't support it directly
                    // Async block or loop is acceptable for this scope
                    for (String key : cacheUpdates.keySet()) {
                        redisTemplate.expire(key, Duration.ofDays(CACHE_TTL_DAYS));
                    }
                } catch (Exception e) {
                    log.error("findBySymbols", "Error updating Redis cache", e);
                }
            }
        }

        return results;
    }

    /**
     * Get a map of Symbol -> Sector
     */
    public Map<String, String> getSymbolToSectorMap(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SecurityDocument> docs = findBySymbols(symbols);
        Map<String, String> sectorMap = new HashMap<>();

        for (SecurityDocument doc : docs) {
            String symbol = doc.getKey() != null ? doc.getKey().getSymbol() : null;
            String sector = (doc.getMetadata() != null && doc.getMetadata().getSector() != null)
                    ? doc.getMetadata().getSector()
                    : "Unknown";

            if (symbol != null) {
                sectorMap.put(symbol, sector);
            }
        }

        // Fill missing as Unknown
        for (String s : symbols) {
            sectorMap.putIfAbsent(s, "Unknown");
        }

        return sectorMap;
    }

    public List<SecurityDocument> getAllSecurities() {
        return securityRepository.findAll();
    }

    public List<SecurityDocument> search(String query) {
        return securityRepository.search(query);
    }

    /**
     * Batch search - process multiple queries at once (supports up to 1000 queries)
     * Uses internal batching and caching for optimal performance
     */
    public com.am.marketdata.common.dto.BatchSearchResponse batchSearch(
            com.am.marketdata.common.dto.BatchSearchRequest request) {

        log.info("batchSearch", "Processing " + request.getQueries().size() + " queries");

        List<com.am.marketdata.common.dto.BatchSearchResponse.QueryResult> results = new ArrayList<>();
        int totalMatches = 0;
        int queriesWithNoMatches = 0;
        int cacheHits = 0;

        // Process in internal batches of 100 for memory efficiency
        int internalBatchSize = 100;
        List<String> queries = request.getQueries();

        for (int batchStart = 0; batchStart < queries.size(); batchStart += internalBatchSize) {
            int batchEnd = Math.min(batchStart + internalBatchSize, queries.size());
            List<String> batchQueries = queries.subList(batchStart, batchEnd);

            log.info("batchSearch", String.format("Processing internal batch %d-%d of %d",
                    batchStart, batchEnd, queries.size()));

            // Check cache first for this batch
            Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> cachedResults = new HashMap<>();
            // checkBatchCache(batchQueries); // DISABLED per user request to force real
            // logic
            cacheHits += cachedResults.size();

            // Separate cached vs uncached queries
            List<String> uncachedQueries = batchQueries.stream()
                    .filter(q -> !cachedResults.containsKey(q))
                    .collect(Collectors.toList());

            // Process uncached queries in bulk
            Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> freshResults = new HashMap<>();

            if (!uncachedQueries.isEmpty()) {
                for (String query : uncachedQueries) {
                    // Try DB Search first (Regex based)
                    List<SecurityDocument> matches = new ArrayList<>(securityRepository.search(query));
                    if (matches != null) {
                        matches.removeIf(doc -> !isValidDocument(doc));
                    }

                    String normalizedQuery = normalizeQuery(query);

                    if (matches.isEmpty() && !normalizedQuery.equals(query)) {
                        List<SecurityDocument> normMatches = securityRepository.search(normalizedQuery);
                        if (normMatches != null) {
                            normMatches.removeIf(doc -> !isValidDocument(doc));
                            matches.addAll(normMatches);
                        }
                    }

                    // FALLBACK: In-Memory Fuzzy Search if DB returned nothing (or only garbage)
                    if (matches.isEmpty()) {
                        log.info("Batch Search: No DB match for '{}', trying in-memory fuzzy fallback...", query);
                        List<SecurityDocument> fuzzyMatches = performInMemoryFuzzySearch(query);
                        matches.addAll(fuzzyMatches); // performInMemoryFuzzySearch already filters ISINs
                        if (!matches.isEmpty()) {
                            log.info("Batch Search: Found {} matches via in-memory fuzzy search", matches.size());
                        }
                    }

                    int limit = request.getLimit() != null ? request.getLimit() : 3;

                    // Optimization: limit processing to top 100 matches
                    List<SecurityDocument> candidateMatches = matches.stream()
                            .distinct() // Ensure no duplicates from multiple strategies
                            .limit(100)
                            .collect(Collectors.toList());

                    List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch> securityMatches = convertToSecurityMatches(
                            query, candidateMatches, request.getMinMatchScore());

                    // Sort by Match Score (desc) -> Market Cap (desc)
                    securityMatches.sort(java.util.Comparator
                            .comparingDouble(
                                    com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch::getMatchScore)
                            .reversed()
                            .thenComparing(m -> m.getMarketCapValue() == null ? 0L : m.getMarketCapValue(),
                                    java.util.Comparator.reverseOrder()));

                    // Apply the final user requested limit
                    if (securityMatches.size() > limit) {
                        securityMatches = securityMatches.subList(0, limit);
                    }

                    freshResults.put(query, securityMatches);
                }

                // Cache fresh results
                // cacheBatchResults(freshResults); // DISABLED per user request
            }

            // Combine cached and fresh results
            Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> allResults = new HashMap<>();
            allResults.putAll(cachedResults);
            allResults.putAll(freshResults);

            // Build query results
            for (String query : batchQueries) {
                List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch> matches = allResults
                        .getOrDefault(query, List.of());

                if (matches.isEmpty()) {
                    queriesWithNoMatches++;
                } else {
                    totalMatches += matches.size();
                }

                results.add(com.am.marketdata.common.dto.BatchSearchResponse.QueryResult.builder()
                        .query(query)
                        .matches(matches)
                        .matchCount(matches.size())
                        .build());
            }
        }

        log.info("batchSearch", String.format("Completed: %d total, %d cache hits, %d matches",
                queries.size(), cacheHits, totalMatches));

        return com.am.marketdata.common.dto.BatchSearchResponse.builder()
                .results(results)
                .totalQueries(request.getQueries().size())
                .totalMatches(totalMatches)
                .queriesWithNoMatches(queriesWithNoMatches)
                .build();
    }

    /**
     * Check Redis cache for batch queries
     */
    private Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> checkBatchCache(
            List<String> queries) {
        Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> cached = new HashMap<>();

        try {
            List<String> cacheKeys = queries.stream()
                    .map(q -> "batch_search:" + q.toLowerCase().trim())
                    .collect(Collectors.toList());

            List<Object> cachedObjects = redisTemplate.opsForValue().multiGet(cacheKeys);

            for (int i = 0; i < queries.size(); i++) {
                Object obj = (cachedObjects != null && cachedObjects.size() > i) ? cachedObjects.get(i) : null;
                if (obj != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch> matches = (List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>) obj;
                        cached.put(queries.get(i), matches);
                    } catch (Exception e) {
                        log.error("checkBatchCache", "Error deserializing cached result for query: " + queries.get(i),
                                e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("checkBatchCache", "Error checking batch cache", e);
        }

        return cached;
    }

    /**
     * Cache batch search results (TTL: 1 hour for regular queries, 24 hours for
     * index searches)
     */
    private void cacheBatchResults(
            Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> results) {
        try {
            Map<String, Object> cacheUpdates = new HashMap<>();

            for (Map.Entry<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> entry : results
                    .entrySet()) {
                String cacheKey = "batch_search:" + entry.getKey().toLowerCase().trim();
                cacheUpdates.put(cacheKey, entry.getValue());
            }
            if (!cacheUpdates.isEmpty()) {
                redisTemplate.opsForValue().multiSet(cacheUpdates);

                // Set expiry - longer for index searches
                for (String key : cacheUpdates.keySet()) {
                    Duration ttl = isIndexQuery(key) ? Duration.ofHours(24) : Duration.ofHours(1);
                    redisTemplate.expire(key, ttl);
                }
            }
        } catch (Exception e) {
            log.error("cacheBatchResults", "Error caching batch results", e);
        }
    }

    /**
     * Normalize query string for better matching
     * Removes common company suffixes and trailing punctuation
     * Converts to lenient regex pattern
     */
    private String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        String normalized = query.trim();

        // Handle Ampersand
        normalized = normalized.replace("&", ".*");

        // Remove trailing periods and commas
        normalized = normalized.replaceAll("[.,;]+$", "");

        // Remove common company suffixes (case-insensitive)
        normalized = normalized
                .replaceAll("(?i)\\s+(Ltd\\.?|Limited|Inc\\.?|Incorporated|Corp\\.?|Corporation|Plc|LLC|LLP)$", "");

        // Remove trailing periods again
        normalized = normalized.replaceAll("[.,;]+$", "").trim();

        // Make punctuation optional or wildcard
        // Replace ' with .? (optional character)
        normalized = normalized.replace("'", ".?");

        // Replace spaces with .* to allow for missing/extra spaces
        normalized = normalized.replaceAll("\\s+", ".*");

        return normalized;
    }

    /**
     * Check if query is for an index (Nifty 50, Nifty Bank, etc.)
     */
    private boolean isIndexQuery(String cacheKey) {
        String keyLower = cacheKey.toLowerCase();
        return keyLower.contains("nifty") || keyLower.contains("sensex") ||
                keyLower.contains("bank") || keyLower.contains("index");
    }

    /**
     * Convert SecurityDocuments to SecurityMatches with scoring
     */
    private List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch> convertToSecurityMatches(
            String query,
            List<SecurityDocument> documents,
            Double minMatchScore) {

        List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch> matches = new ArrayList<>();

        for (SecurityDocument doc : documents) {
            double matchScore = calculateMatchScore(query, doc);

            // Filter by minimum score
            if (minMatchScore != null && matchScore < minMatchScore) {
                continue;
            }

            String matchedField = determineMatchedField(query, doc);

            matches.add(com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch.builder()
                    .symbol(doc.getKey() != null ? doc.getKey().getSymbol() : null)
                    .isin(doc.getKey() != null ? doc.getKey().getIsin() : null)
                    .companyName(doc.getMetadata() != null ? doc.getMetadata().getCompanyName() : null)
                    .sector(doc.getMetadata() != null ? doc.getMetadata().getSector() : null)
                    .industry(doc.getMetadata() != null ? doc.getMetadata().getIndustry() : null)
                    .matchScore(matchScore)
                    .matchedField(matchedField)
                    .marketCapValue(doc.getMetadata() != null ? doc.getMetadata().getMarketCapValue() : null)
                    .marketCapType(doc.getMetadata() != null ? doc.getMetadata().getMarketCapType() : null)
                    .build());
        }

        return matches;
    }

    /**
     * Calculate simple match score based on text similarity
     */
    private double calculateMatchScore(String query, SecurityDocument doc) {
        String queryLower = query.toLowerCase().trim();

        // Exact matches
        if (doc.getKey() != null && doc.getKey().getSymbol() != null) {
            if (queryLower.equals(doc.getKey().getSymbol().toLowerCase())) {
                return 1.0;
            }
        }

        if (doc.getKey() != null && doc.getKey().getIsin() != null) {
            if (queryLower.equals(doc.getKey().getIsin().toLowerCase())) {
                return 1.0;
            }
        }

        if (doc.getMetadata() != null && doc.getMetadata().getCompanyName() != null) {
            String companyNameLower = doc.getMetadata().getCompanyName().toLowerCase();
            if (queryLower.equals(companyNameLower)) {
                return 1.0;
            }

            // Partial match - contains
            if (companyNameLower.contains(queryLower)) {
                return 0.9;
            }

            // Reverse - query contains company name
            if (queryLower.contains(companyNameLower)) {
                return 0.85;
            }
        }

        // Default fuzzy match score
        return 0.7;
    }

    /**
     * Determine which field was matched
     */
    private String determineMatchedField(String query, SecurityDocument doc) {
        String queryLower = query.toLowerCase().trim();

        if (doc.getKey() != null) {
            if (doc.getKey().getSymbol() != null &&
                    doc.getKey().getSymbol().toLowerCase().contains(queryLower)) {
                return "SYMBOL";
            }
            if (doc.getKey().getIsin() != null &&
                    doc.getKey().getIsin().toLowerCase().contains(queryLower)) {
                return "ISIN";
            }
        }

        if (doc.getMetadata() != null && doc.getMetadata().getCompanyName() != null &&
                doc.getMetadata().getCompanyName().toLowerCase().contains(queryLower)) {
            return "COMPANY_NAME";
        }

        return "FUZZY";
    }

    public List<SecurityDocument> search(SecuritySearchRequest request) {
        // Optimization: If symbols are known (e.g. from Index lookup), use cached
        // finder
        if (request.getSymbols() != null && !request.getSymbols().isEmpty()) {
            List<SecurityDocument> docs = findBySymbols(request.getSymbols());

            // Apply in-memory filtering for other fields
            return docs.stream()
                    .filter(d -> {
                        if (d == null)
                            return false;
                        boolean match = true;
                        if (request.getIsin() != null && !request.getIsin().isEmpty()) {
                            match &= d.getKey() != null && request.getIsin().equals(d.getKey().getIsin());
                        }
                        if (request.getSector() != null && !request.getSector().isEmpty()) {
                            match &= d.getMetadata() != null
                                    && request.getSector().equalsIgnoreCase(d.getMetadata().getSector());
                        }
                        if (request.getIndustry() != null && !request.getIndustry().isEmpty()) {
                            match &= d.getMetadata() != null
                                    && request.getIndustry().equalsIgnoreCase(d.getMetadata().getIndustry());
                        }
                        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                            String q = request.getQuery().toLowerCase();
                            boolean symbolMatch = d.getKey() != null
                                    && d.getKey().getSymbol().toLowerCase().contains(q);
                            // Safe check for ISIN
                            boolean isinMatch = d.getKey() != null && d.getKey().getIsin() != null
                                    && d.getKey().getIsin().toLowerCase().contains(q);
                            match &= (symbolMatch || isinMatch);
                        }
                        return match;
                    })
                    .collect(Collectors.toList());
        }

        Query query = new Query();

        if (request.getIsin() != null && !request.getIsin().isEmpty()) {
            query.addCriteria(Criteria.where("key.isin").is(request.getIsin()));
        }

        if (request.getSector() != null && !request.getSector().isEmpty()) {
            query.addCriteria(Criteria.where("metadata.sector").is(request.getSector()));
        }

        if (request.getIndustry() != null && !request.getIndustry().isEmpty()) {
            query.addCriteria(Criteria.where("metadata.industry").is(request.getIndustry()));
        }

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            // Regex search for symbol or ISIN
            String regex = request.getQuery();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("key.symbol").regex(regex, "i"),
                    Criteria.where("key.isin").regex(regex, "i")));
        }

        return mongoTemplate.find(query, SecurityDocument.class);
    }

    // In-Memory Search Fallback

    // Cache for all securities
    private java.util.concurrent.atomic.AtomicReference<List<SecurityDocument>> cachedAllSecurities = new java.util.concurrent.atomic.AtomicReference<>();
    private long lastCacheTime = 0;

    private void refreshSecurityCache() {
        long now = System.currentTimeMillis();
        // Refresh if empty or older than 24 hours
        if (cachedAllSecurities.get() == null || (now - lastCacheTime > 86400000L)) {
            synchronized (this) {
                if (cachedAllSecurities.get() == null || (now - lastCacheTime > 86400000L)) {
                    log.info("refreshSecurityCache", "Loading all securities for in-memory fallback...");
                    List<SecurityDocument> all = securityRepository.findAll();
                    cachedAllSecurities.set(all);
                    lastCacheTime = now;
                    log.info("refreshSecurityCache", "Loaded " + all.size() + " securities.");
                }
            }
        }
    }

    private boolean isValidDocument(SecurityDocument doc) {
        if (doc == null || doc.getKey() == null)
            return false;

        String isin = doc.getKey().getIsin();
        String symbol = doc.getKey().getSymbol();

        // Filter out invalid ISINs
        if (isin == null || isin.trim().isEmpty() || "-".equals(isin.trim()) || "NA".equalsIgnoreCase(isin.trim())) {
            return false;
        }

        // Filter out invalid Symbols
        if (symbol == null || symbol.trim().isEmpty() || "-".equals(symbol.trim())
                || "NA".equalsIgnoreCase(symbol.trim())) {
            return false;
        }

        return true;
    }

    private List<SecurityDocument> performInMemoryFuzzySearch(String query) {
        List<SecurityDocument> matches = new ArrayList<>();
        String normalizedQuery = cleanString(query);

        if (normalizedQuery.length() < 3)
            return Collections.emptyList();

        log.info("performInMemoryFuzzySearch", "Fetching NIFTY 500 data for fuzzy matching...");
        // 1. Fetch NIFTY 500 Data
        try {
            List<com.am.common.investment.model.stockindice.StockIndicesMarketData> indicesData = stockIndicesMarketDataService
                    .findByIndexSymbols(java.util.Collections.singleton("NIFTY 500"));

            if (!indicesData.isEmpty() && indicesData.get(0).getData() != null) {
                List<com.am.common.investment.model.stockindice.StockData> stockDataList = indicesData.get(0).getData();
                log.info("performInMemoryFuzzySearch",
                        "Comparing against " + stockDataList.size() + " NIFTY 500 stocks.");

                for (com.am.common.investment.model.stockindice.StockData stock : stockDataList) {
                    // Filter out if ISIN is null (as per requirement)
                    if (stock.getIsin() == null || stock.getIsin().isEmpty())
                        continue;

                    String stockName = cleanString(stock.getCompanyName());
                    if (stockName.isEmpty())
                        continue;

                    // Fuzzy Match Logic
                    if (stockName.contains(normalizedQuery) || normalizedQuery.contains(stockName)) {
                        // Build SecurityDocument from StockData
                        SecurityDocument doc = new SecurityDocument();

                        SecurityDocument.SecurityKey key = new SecurityDocument.SecurityKey();
                        key.setSymbol(stock.getSymbol());
                        key.setIsin(stock.getIsin());
                        doc.setKey(key);

                        SecurityDocument.SecurityMetadata metadata = new SecurityDocument.SecurityMetadata();
                        metadata.setCompanyName(stock.getCompanyName());
                        // metadata.setSector(stock.getSector()); // StockData does not have sector
                        metadata.setIndustry(stock.getIndustry());
                        doc.setMetadata(metadata);

                        matches.add(doc);
                    }
                }

                if (!matches.isEmpty()) {
                    log.info("performInMemoryFuzzySearch", "Found " + matches.size() + " matches in NIFTY 500.");
                    return matches;
                }
            }
        } catch (Exception e) {
            log.error("performInMemoryFuzzySearch", "Error fetching NIFTY 500 data", e);
        }

        // 2. Fallback to All Cached Securities (if enabled/populated)
        refreshSecurityCache();
        List<SecurityDocument> all = cachedAllSecurities.get();
        if (all == null || all.isEmpty())
            return Collections.emptyList();

        List<SecurityDocument> cachedMatches = all.stream()
                .filter(doc -> {
                    if (doc.getKey() == null || doc.getKey().getIsin() == null)
                        return false; // Filter null ISIN
                    if (doc.getMetadata() == null || doc.getMetadata().getCompanyName() == null)
                        return false;
                    String companyName = cleanString(doc.getMetadata().getCompanyName());

                    return companyName.contains(normalizedQuery) || normalizedQuery.contains(companyName);
                })
                .limit(20)
                .collect(Collectors.toList());

        if (!cachedMatches.isEmpty()) {
            return cachedMatches;
        }

        // 3. Fallback to Manual JSON Update List
        return performManualSearch(normalizedQuery);
    }

    private List<SecurityDocument> performManualSearch(String query) {
        if (manualSecurityUpdates.isEmpty()) {
            return Collections.emptyList();
        }

        List<SecurityDocument> matches = new ArrayList<>();
        String normalizedQuery = cleanString(query);

        for (ManualSecurityUpdate update : manualSecurityUpdates) {
            if (update.getCompanyName() == null)
                continue;

            String updateName = cleanString(update.getCompanyName());
            // Exact or very close match preferred for manual list
            if (updateName.contains(normalizedQuery) || normalizedQuery.contains(updateName)) {
                SecurityDocument doc = new SecurityDocument();

                SecurityDocument.SecurityKey key = new SecurityDocument.SecurityKey();
                key.setIsin(update.getIsin());
                // Symbol might be unknown, but we need ISIN mostly
                key.setSymbol("MANUAL-" + (update.getIsin() != null ? update.getIsin() : "UNKNOWN"));
                doc.setKey(key);

                SecurityDocument.SecurityMetadata metadata = new SecurityDocument.SecurityMetadata();
                metadata.setCompanyName(update.getCompanyName());
                metadata.setMarketCapType(update.getMarketCapType());
                metadata.setMarketCapValue(update.getMarketCapValue());

                doc.setMetadata(metadata);
                matches.add(doc);
            }
        }

        if (!matches.isEmpty()) {
            log.info("performManualSearch",
                    "Found " + matches.size() + " matches in manual updates for query: " + query);
        }

        return matches;
    }

    private String cleanString(String s) {
        if (s == null)
            return "";
        // Lowercase, remove all non-alphanumeric characters
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
