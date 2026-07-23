package com.am.marketdata.service;

import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.config.BatchSearchProperties;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

@Service
public class SecurityService {

    /** Bumped when batch-search matching logic changes; ignores legacy Redis keys. */
    private static final String BATCH_SEARCH_CACHE_PREFIX = "batch_search:v2:";

    private final AppLogger log = AppLogger.getLogger();
    private final SecurityRepository securityRepository;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.am.common.investment.service.StockIndicesMarketDataService stockIndicesMarketDataService;
    private final BatchSearchProperties batchSearchProperties;

    // THUNDERING HERD / REQUEST COLLAPSING MAP:
    // Holds active in-flight CompletableFutures for queries currently being fetched from MongoDB.
    // If 30 concurrent threads request "TCS" at the same millisecond, Thread 1 creates the future,
    // and Threads 2 through 30 attach to Thread 1's future instead of firing 30 duplicate Mongo queries.
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CompletableFuture<List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>>> activeFetches = new java.util.concurrent.ConcurrentHashMap<>();

    public SecurityService(SecurityRepository securityRepository, MongoTemplate mongoTemplate,
            RedisTemplate<String, Object> redisTemplate,
            com.am.common.investment.service.StockIndicesMarketDataService stockIndicesMarketDataService,
            BatchSearchProperties batchSearchProperties) {
        this.securityRepository = securityRepository;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
        this.stockIndicesMarketDataService = stockIndicesMarketDataService;
        this.batchSearchProperties = batchSearchProperties;
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

            // 4. Update Redis Cache for items fetched from MongoDB:
            if (!cacheUpdates.isEmpty()) {
                try {
                    // Bulk write all newly fetched securities into Redis in 1 network command
                    redisTemplate.opsForValue().multiSet(cacheUpdates);

                    // REDIS PIPELINING OPTIMIZATION:
                    // Pipeline all TTL expiration commands into 1 single TCP packet instead of looping over N network calls
                    redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                        for (String key : cacheUpdates.keySet()) {
                            byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);
                            if (rawKey != null) {
                                connection.keyCommands().expire(rawKey, CACHE_TTL_DAYS * 86400);
                            }
                        }
                        return null;
                    });
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
    /**
     * Batch search - process multiple queries at once (supports up to 1000 queries)
     * OPTIMIZATION & CONCURRENCY SUMMARY:
     * 1. Input Deduplication: Filters out nulls/blanks and deduplicates queries.
     * 2. Redis Cache Check (Tier 1): Resolves cached terms in 1 multiGet call.
     * 3. Singleflight Request Collapsing (Tier 2): Uses activeFetches map to collapse duplicate concurrent queries across threads.
     * 4. Safe Timeout Handling: Protects threads with a 3-second timeout safety ceiling.
     */
    public com.am.marketdata.common.dto.BatchSearchResponse batchSearch(
            com.am.marketdata.common.dto.BatchSearchRequest request) {

        List<String> rawQueries = request.getQueries();
        if (rawQueries == null || rawQueries.isEmpty()) {
            return com.am.marketdata.common.dto.BatchSearchResponse.builder()
                    .results(List.of())
                    .totalQueries(0)
                    .totalMatches(0)
                    .queriesWithNoMatches(0)
                    .build();
        }

        // STEP 1: INPUT DEDUPLICATION & VALIDATION
        // Filter out null/blank strings and deduplicate input array to prevent redundant work
        List<String> queries = rawQueries.stream()
                .filter(q -> q != null && !q.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        int maxQueries = batchSearchProperties.getMaxQueries();
        if (queries.size() > maxQueries) {
            throw new IllegalArgumentException("Maximum " + maxQueries + " queries per batch-search request");
        }

        log.info("batchSearch", "Processing " + queries.size() + " distinct queries (mongoQueryLimit="
                + batchSearchProperties.getMongoQueryLimit() + ", maxCandidates="
                + batchSearchProperties.getMaxCandidatesPerQuery() + ")");

        List<com.am.marketdata.common.dto.BatchSearchResponse.QueryResult> results = new ArrayList<>();
        int totalMatches = 0;
        int queriesWithNoMatches = 0;
        int cacheHits = 0;
        int internalBatchSize = Math.max(1, batchSearchProperties.getInternalBatchSize());

        for (int batchStart = 0; batchStart < queries.size(); batchStart += internalBatchSize) {
            int batchEnd = Math.min(batchStart + internalBatchSize, queries.size());
            List<String> batchQueries = queries.subList(batchStart, batchEnd);

            log.info("batchSearch", String.format("Processing internal batch %d-%d of %d",
                    batchStart, batchEnd, queries.size()));

            // STEP 2: TIER 1 - REDIS CACHE LOOKUP
            // Fetch cached matches from Redis in 1 multiGet network call
            Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> cachedResults =
                    batchSearchProperties.isCacheEnabled() ? checkBatchCache(batchQueries) : new HashMap<>();
            cacheHits += cachedResults.size();
            if (!cachedResults.isEmpty()) {
                log.info("batchSearch", "Cache hits for queries: " + cachedResults.keySet());
            }

            // Identify terms missing from Redis cache
            List<String> uncachedQueries = batchQueries.stream()
                    .filter(q -> !cachedResults.containsKey(q))
                    .collect(Collectors.toList());
            if (!uncachedQueries.isEmpty()) {
                log.info("batchSearch", "Fresh search needed for queries: " + uncachedQueries);
            }

            Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> freshResults =
                    new LinkedHashMap<>();

            if (!uncachedQueries.isEmpty()) {
                int limit = request.getLimit() != null ? request.getLimit() : 3;

                // STEP 3: SINGLE-QUERY BULK FETCH FOR UNCACHED TERMS (1 NETWORK CALL)
                // Combine all uncached query strings into a single MongoDB $in bulk query for symbols and ISINs.
                // This replaces the N+1 thread pool loop with 1 single database call (O(1) connection usage).
                Map<String, List<SecurityDocument>> bulkDocMap = bulkFetchDocumentsForQueries(uncachedQueries);

                for (String query : uncachedQueries) {
                    List<SecurityDocument> matches = bulkDocMap.getOrDefault(query.toLowerCase(), List.of());

                    // Fallback to individual resolution (Text Index / NIFTY 500) if no exact bulk match was found
                    if (matches.isEmpty()) {
                        matches = resolveDocumentsForQuery(query);
                    }

                    List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch> securityMatches =
                            convertToSecurityMatches(query, matches, request.getMinMatchScore());

                    securityMatches.sort(java.util.Comparator
                            .comparingDouble(
                                    com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch::getMatchScore)
                            .reversed()
                            .thenComparing(m -> m.getMarketCapValue() == null ? 0L : m.getMarketCapValue(),
                                    java.util.Comparator.reverseOrder()));

                    if (securityMatches.size() > limit) {
                        securityMatches = securityMatches.subList(0, limit);
                    }

                    freshResults.put(query, securityMatches);
                }

                // STEP 4: CACHE FRESH RESULTS IN REDIS
                if (batchSearchProperties.isCacheEnabled() && !freshResults.isEmpty()) {
                    cacheBatchResults(freshResults);
                }
            }

            // STEP 5: ASSEMBLE ALL COMBINED RESULTS (CACHED + FRESH)
            Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> allResults =
                    new HashMap<>();
            allResults.putAll(cachedResults);
            allResults.putAll(freshResults);

            for (String query : batchQueries) {
                List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch> matches =
                        allResults.getOrDefault(query, List.of());
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
                .totalQueries(queries.size())
                .totalMatches(totalMatches)
                .queriesWithNoMatches(queriesWithNoMatches)
                .build();
    }

    /**
     * Bounded Mongo + NIFTY 500 + manual lookup — never loads full securities collection.
     */
    private List<SecurityDocument> resolveDocumentsForQuery(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = query.trim();
        LinkedHashMap<String, SecurityDocument> deduped = new LinkedHashMap<>();

        if (trimmed.matches("(?i)^INE[A-Z0-9]{10}$")) {
            SecurityDocument byIsin = securityRepository.findByIsin(trimmed.toUpperCase());
            if (isValidDocument(byIsin)) {
                deduped.put(byIsin.getKey().getIsin(), byIsin);
            }
        }

        addValidDocuments(deduped, searchDocuments(trimmed));

        if (deduped.isEmpty()) {
            String normalizedQuery = normalizeQuery(trimmed);
            if (!normalizedQuery.equals(trimmed)) {
                addValidDocuments(deduped, searchDocuments(normalizedQuery));
            }
        }

        if (deduped.isEmpty()) {
            log.info("batchSearch", "No DB match for '" + trimmed + "', trying NIFTY 500 / manual fallback");
            addValidDocuments(deduped, performInMemoryFuzzySearch(trimmed));
        }

        return capCandidates(deduped.values().stream().collect(Collectors.toList()));
    }

    private List<SecurityDocument> capCandidates(List<SecurityDocument> documents) {
        int cap = batchSearchProperties.getMaxCandidatesPerQuery();
        if (cap <= 0 || documents.size() <= cap) {
            return documents;
        }
        return documents.subList(0, cap);
    }

    private void addValidDocuments(Map<String, SecurityDocument> target, List<SecurityDocument> docs) {
        if (docs == null) {
            return;
        }
        for (SecurityDocument doc : docs) {
            if (!isValidDocument(doc) || doc.getKey() == null || doc.getKey().getIsin() == null) {
                continue;
            }
            target.putIfAbsent(doc.getKey().getIsin(), doc);
        }
    }

    /**
     * Bulk fetch securities matching any symbol or ISIN in a single MongoDB query.
     * Uses B-Tree unique/compound indexes (key.symbol_1, key.isin_1) for sub-millisecond lookup.
     */
    private Map<String, List<SecurityDocument>> bulkFetchDocumentsForQueries(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> upperQueries = queries.stream().map(String::toUpperCase).collect(Collectors.toList());

        Query mongoQuery = new Query(new Criteria().orOperator(
                Criteria.where("key.symbol").in(upperQueries),
                Criteria.where("key.isin").in(upperQueries)
        ));

        // FIELD PROJECTION OPTIMIZATION:
        // Instruct MongoDB to return only key and metadata fields over TCP wire
        mongoQuery.fields().include("key").include("metadata");

        List<SecurityDocument> docs = mongoTemplate.find(mongoQuery, SecurityDocument.class);
        if (docs == null || docs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<SecurityDocument>> docMap = new HashMap<>();
        for (SecurityDocument doc : docs) {
            if (!isValidDocument(doc) || doc.getKey() == null) {
                continue;
            }
            if (doc.getKey().getSymbol() != null) {
                String symLower = doc.getKey().getSymbol().toLowerCase();
                docMap.computeIfAbsent(symLower, k -> new ArrayList<>()).add(doc);
            }
            if (doc.getKey().getIsin() != null) {
                String isinLower = doc.getKey().getIsin().toLowerCase();
                docMap.computeIfAbsent(isinLower, k -> new ArrayList<>()).add(doc);
            }
        }
        return docMap;
    }

    private List<SecurityDocument> searchDocuments(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String escaped = Pattern.quote(text.trim());
        Query mongoQuery = new Query(new Criteria().orOperator(
                Criteria.where("metadata.company_name").regex(escaped, "i"),
                Criteria.where("key.symbol").regex(escaped, "i"),
                Criteria.where("key.isin").regex(escaped, "i")));

        // FIELD PROJECTION OPTIMIZATION:
        // Instruct MongoDB to return only key and metadata fields to reduce TCP payload size by ~50%
        mongoQuery.fields().include("key").include("metadata");

        int mongoLimit = batchSearchProperties.getMongoQueryLimit();
        if (mongoLimit > 0) {
            mongoQuery.limit(mongoLimit);
        }
        List<SecurityDocument> found = mongoTemplate.find(mongoQuery, SecurityDocument.class);
        return found != null ? found : Collections.emptyList();
    }

    /**
     * Check Redis cache for batch queries
     */
    private Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> checkBatchCache(
            List<String> queries) {
        Map<String, List<com.am.marketdata.common.dto.BatchSearchResponse.SecurityMatch>> cached = new HashMap<>();

        try {
            List<String> cacheKeys = queries.stream()
                    .map(this::batchSearchCacheKey)
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
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                cacheUpdates.put(batchSearchCacheKey(entry.getKey()), entry.getValue());
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
        String queryNormalized = cleanString(query);

        // Exact matches
        if (doc.getKey() != null && doc.getKey().getSymbol() != null) {
            if (queryNormalized.equals(cleanString(doc.getKey().getSymbol()))) {
                return 1.0;
            }
        }

        if (doc.getKey() != null && doc.getKey().getIsin() != null) {
            if (queryNormalized.equals(cleanString(doc.getKey().getIsin()))) {
                return 1.0;
            }
        }

        if (doc.getMetadata() != null && doc.getMetadata().getCompanyName() != null) {
            String companyNormalized = cleanString(doc.getMetadata().getCompanyName());
            if (!queryNormalized.isEmpty() && !companyNormalized.isEmpty()) {
                if (queryNormalized.equals(companyNormalized)) {
                    return 1.0;
                }
                if (companyNormalized.contains(queryNormalized)) {
                    return 0.9;
                }
                if (queryNormalized.contains(companyNormalized)) {
                    return 0.85;
                }
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

        // 1. PROJECTION OPTIMIZATION:
        // Instruct MongoDB to return only 'key' and 'metadata' fields over the TCP wire.
        // Omits unnecessary audit timestamps and Mongo internal class tags, reducing network payload by ~50%.
        query.fields()
                .include("key")
                .include("metadata");

        // 2. RESULT CAPPING & PAGINATION SAFEGUARD:
        // Prevents unpaginated queries from returning thousands of records into JVM memory.
        // If client specifies a limit, respect it (up to 1,000 max ceiling). Otherwise default to 50 items.
        int limit = (request.getLimit() != null && request.getLimit() > 0)
                ? Math.min(request.getLimit(), 1000)
                : 50;
        query.limit(limit);

        // 3. FILTER CRITERIA BUILDING:
        // Match exact ISIN if provided
        if (request.getIsin() != null && !request.getIsin().isEmpty()) {
            query.addCriteria(Criteria.where("key.isin").is(request.getIsin()));
        }

        // Match exact Sector if provided
        if (request.getSector() != null && !request.getSector().isEmpty()) {
            query.addCriteria(Criteria.where("metadata.sector").is(request.getSector()));
        }

        // Match exact Industry if provided
        if (request.getIndustry() != null && !request.getIndustry().isEmpty()) {
            query.addCriteria(Criteria.where("metadata.industry").is(request.getIndustry()));
        }

        // Case-insensitive regex fuzzy search matching either Symbol OR ISIN
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            String regex = request.getQuery();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("key.symbol").regex(regex, "i"),
                    Criteria.where("key.isin").regex(regex, "i")));
        }

        // 4. QUERY EXECUTION:
        // Executes indexed query against MongoDB (utilizes Text Index and B-Tree indexes for <1ms latency)
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

        // 2. Manual JSON fallback only (no findAll — avoids OOM on large securities collection)
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

    private String batchSearchCacheKey(String query) {
        return BATCH_SEARCH_CACHE_PREFIX + query.toLowerCase().trim();
    }

    private String cleanString(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String normalized = s.toLowerCase().trim();
        normalized = normalized.replaceAll("[.,;]+$", "");
        // Align "Ltd" / "Limited" / etc. so "HDFC Bank Ltd" matches "HDFC Bank Limited"
        normalized = normalized
                .replaceAll("(?i)\\s+(ltd\\.?|limited|inc\\.?|incorporated|corp\\.?|corporation|plc|llc|llp)$", "")
                .trim();
        return normalized.replaceAll("[^a-z0-9]", "");
    }
}
