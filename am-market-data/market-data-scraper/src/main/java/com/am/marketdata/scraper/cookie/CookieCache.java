package com.am.marketdata.scraper.cookie;

import com.am.marketdata.scraper.exception.CookieException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class CookieCache {
    static final String REDIS_COOKIE_KEY = "nse:cookies";
    static final String REDIS_STORED_AT_KEY = "nse:cookies:stored_at";
    private static final String L1_KEY = "nse_cookies";

    private final Cache<String, String> localCache;
    private final StringRedisTemplate redisTemplate;
    private final int ttlMinutes;

    public CookieCache(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${market-data.nse.cookies.ttl-minutes:60}") int ttlMinutes) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.ttlMinutes = Math.max(1, ttlMinutes);
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(this.ttlMinutes))
                .build();
        if (this.redisTemplate == null) {
            log.warn("StringRedisTemplate unavailable; NSE cookies will use local cache only");
        }
    }

    public void storeCookies(String cookies) {
        storeCookies(cookies, ttlMinutes);
    }

    public void storeCookies(String cookies, int ttlMinutesOverride) {
        if (cookies == null || cookies.isBlank()) {
            throw new CookieException("Cannot store empty NSE cookie string");
        }
        int ttl = Math.max(1, ttlMinutesOverride);
        String trimmed = cookies.trim();
        Instant storedAt = Instant.now();
        localCache.put(L1_KEY, trimmed);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_COOKIE_KEY, trimmed, ttl, TimeUnit.MINUTES);
                redisTemplate.opsForValue().set(REDIS_STORED_AT_KEY, storedAt.toString(), ttl, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Failed to write NSE cookies to Redis: {}", e.getMessage());
            }
        }
        log.info("Stored NSE cookies (ttlMinutes={}): {}", ttl, maskCookieValues(trimmed));
    }

    public String getCookies() {
        String local = localCache.getIfPresent(L1_KEY);
        if (local != null && !local.isBlank()) {
            return local;
        }
        if (redisTemplate == null) {
            return null;
        }
        try {
            String fromRedis = redisTemplate.opsForValue().get(REDIS_COOKIE_KEY);
            if (fromRedis != null && !fromRedis.isBlank()) {
                localCache.put(L1_KEY, fromRedis);
                log.debug("Loaded NSE cookies from Redis into L1: {}", maskCookieValues(fromRedis));
                return fromRedis;
            }
        } catch (Exception e) {
            log.warn("Failed to read NSE cookies from Redis: {}", e.getMessage());
        }
        return null;
    }

    public String getCookiesOrThrow() {
        String cookies = getCookies();
        if (cookies == null || cookies.isBlank()) {
            throw new CookieException(
                    "No NSE cookies in Redis/cache. PUT /v1/admin/nse/cookies or run cookie scheduler refresh");
        }
        return cookies;
    }

    public void invalidateCookies() {
        String cookies = getCookies();
        if (cookies != null) {
            log.info("Invalidating NSE cookies: {}", maskCookieValues(cookies));
        }
        localCache.invalidate(L1_KEY);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(List.of(REDIS_COOKIE_KEY, REDIS_STORED_AT_KEY));
            } catch (Exception e) {
                log.warn("Failed to delete NSE cookies from Redis: {}", e.getMessage());
            }
        }
    }

    public CookiePresenceStatus status() {
        String cookies = getCookies();
        Instant storedAt = readStoredAt();
        Long ttlSeconds = null;
        if (redisTemplate != null) {
            try {
                ttlSeconds = redisTemplate.getExpire(REDIS_COOKIE_KEY, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("Could not read cookie TTL: {}", e.getMessage());
            }
        }
        boolean present = cookies != null && !cookies.isBlank();
        List<String> names = present ? cookieNames(cookies) : List.of();
        return new CookiePresenceStatus(
                present,
                names,
                storedAt,
                ttlSeconds == null || ttlSeconds < 0 ? null : ttlSeconds,
                redisTemplate != null);
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    private Instant readStoredAt() {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String raw = redisTemplate.opsForValue().get(REDIS_STORED_AT_KEY);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return Instant.parse(raw);
        } catch (Exception e) {
            log.debug("Could not parse cookie stored_at: {}", e.getMessage());
            return null;
        }
    }

    private static List<String> cookieNames(String cookies) {
        return Arrays.stream(cookies.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(cookie -> {
                    int eq = cookie.indexOf('=');
                    return eq > 0 ? cookie.substring(0, eq).trim() : cookie;
                })
                .filter(s -> !s.isEmpty())
                .toList();
    }

    static String maskCookieValues(String cookies) {
        if (cookies == null) {
            return null;
        }
        return Stream.of(cookies.split(";"))
                .map(cookie -> {
                    String[] parts = cookie.split("=", 2);
                    return parts.length > 1
                            ? parts[0].trim() + "=*****"
                            : cookie.trim() + "=*****";
                })
                .collect(Collectors.joining("; "));
    }

    public record CookiePresenceStatus(
            boolean present,
            List<String> cookieNames,
            Instant storedAt,
            Long ttlSecondsRemaining,
            boolean redisBacked) {
    }
}
