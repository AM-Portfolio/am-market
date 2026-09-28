package com.am.marketdata.service.redis;

import com.am.marketdata.common.model.ipo.AsraxIpoCountsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoDetailsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoSummaryDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Redis Cache Manager for Upstox IPO data.
 * Implements strict try-catch exception resiliency so that Redis outages, drops, or locks
 * never throw errors to callers or break API availability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpstoxIpoCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX_LIST = "ipo:upstox:list:";
    private static final String CACHE_PREFIX_DETAIL = "ipo:upstox:detail:";
    private static final String CACHE_KEY_COUNTS = "ipo:upstox:counts";

    /**
     * Attempts to read cached IPO summary counts from Redis.
     */
    public Optional<AsraxIpoCountsDto> getCachedCounts() {
        try {
            String json = stringRedisTemplate.opsForValue().get(CACHE_KEY_COUNTS);
            if (json != null && !json.trim().isEmpty()) {
                AsraxIpoCountsDto dto = objectMapper.readValue(json, AsraxIpoCountsDto.class);
                log.debug("Redis HIT for key: {}", CACHE_KEY_COUNTS);
                return Optional.of(dto);
            }
        } catch (Throwable e) {
            log.warn("Redis read exception for counts key '{}': {}. Falling back to DB.", CACHE_KEY_COUNTS, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Stores IPO summary counts into Redis cache with 24h TTL.
     */
    public void cacheCounts(AsraxIpoCountsDto counts) {
        if (counts == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(counts);
            stringRedisTemplate.opsForValue().set(CACHE_KEY_COUNTS, json, Duration.ofHours(24));
            log.debug("Redis SET key: {} with TTL: 24h", CACHE_KEY_COUNTS);
        } catch (Throwable e) {
            log.warn("Redis write exception for counts key '{}': {}", CACHE_KEY_COUNTS, e.getMessage());
        }
    }


    /**
     * Attempts to read cached IPO summary listing from Redis.
     */
    public Optional<List<AsraxIpoSummaryDto>> getCachedList(String status, String issueType, Integer page, Integer records) {
        String key = buildListKey(status, issueType, page, records);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null && !json.trim().isEmpty()) {
                List<AsraxIpoSummaryDto> list = objectMapper.readValue(json, new TypeReference<List<AsraxIpoSummaryDto>>() {});
                log.debug("Redis HIT for key: {}", key);
                return Optional.of(list);
            }
        } catch (Throwable e) {
            log.warn("Redis read exception for key '{}': {}. Falling back to DB.", key, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Stores IPO summary listing into Redis cache with dynamic TTL.
     */
    public void cacheList(String status, String issueType, Integer page, Integer records, List<AsraxIpoSummaryDto> data) {
        if (data == null) {
            return;
        }
        String key = buildListKey(status, issueType, page, records);
        Duration ttl = calculateTtl(status);

        try {
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Redis SET key: {} with TTL: {}s", key, ttl.getSeconds());
        } catch (Throwable e) {
            log.warn("Redis write exception for key '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Attempts to read cached IPO details from Redis.
     */
    public Optional<AsraxIpoDetailsDto> getCachedDetails(String ipoId) {
        if (ipoId == null) {
            return Optional.empty();
        }
        String key = CACHE_PREFIX_DETAIL + ipoId.trim().toLowerCase();
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null && !json.trim().isEmpty()) {
                AsraxIpoDetailsDto dto = objectMapper.readValue(json, AsraxIpoDetailsDto.class);
                log.debug("Redis HIT for detail key: {}", key);
                return Optional.of(dto);
            }
        } catch (Throwable e) {
            log.warn("Redis read exception for detail key '{}': {}. Falling back to DB.", key, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Stores IPO details into Redis cache with dynamic TTL.
     */
    public void cacheDetails(String ipoId, AsraxIpoDetailsDto dto) {
        if (ipoId == null || dto == null) {
            return;
        }
        String key = CACHE_PREFIX_DETAIL + ipoId.trim().toLowerCase();
        Duration ttl = calculateTtl(dto.getStatus());

        try {
            String json = objectMapper.writeValueAsString(dto);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
            log.debug("Redis SET detail key: {} with TTL: {}s", key, ttl.getSeconds());
        } catch (Throwable e) {
            log.warn("Redis write exception for detail key '{}': {}", key, e.getMessage());
        }
    }

    private String buildListKey(String status, String issueType, Integer page, Integer records) {
        String s = (status != null) ? status.toLowerCase() : "open";
        String t = (issueType != null) ? issueType.toLowerCase() : "all";
        int p = (page != null) ? page : 1;
        int r = (records != null) ? records : 30;
        return CACHE_PREFIX_LIST + s + ":" + t + ":" + p + ":" + r;
    }

    /**
     * Calculates dynamic TTL based on status and market bidding hours (10:00 - 17:00 IST).
     */
    private Duration calculateTtl(String status) {
        if ("open".equalsIgnoreCase(status)) {
            LocalTime now = LocalTime.now();
            // Active bidding window 10:00 - 17:00 IST -> 15 min TTL; Off-hours -> 1 hr TTL
            if (now.isAfter(LocalTime.of(9, 55)) && now.isBefore(LocalTime.of(17, 10))) {
                return Duration.ofMinutes(15);
            }
            return Duration.ofHours(1);
        }
        // Upcoming, Closed, Listed -> 24 hours TTL
        return Duration.ofHours(24);
    }
}
