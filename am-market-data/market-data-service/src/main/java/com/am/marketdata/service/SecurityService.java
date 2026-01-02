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
import org.springframework.data.redis.core.RedisTemplate;
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

    public SecurityService(SecurityRepository securityRepository, MongoTemplate mongoTemplate,
            RedisTemplate<String, Object> redisTemplate) {
        this.securityRepository = securityRepository;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
}
