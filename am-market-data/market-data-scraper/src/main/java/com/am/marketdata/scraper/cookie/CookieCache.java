package com.am.marketdata.scraper.cookie;

import com.am.marketdata.scraper.exception.CookieException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages cookie caching with expiration
 */
@Component
@Slf4j
public class CookieCache {
    private static final String NSE_COOKIE_KEY = "nse_cookies";
    private final Cache<String, String> cookieCache;

    public CookieCache() {
        this.cookieCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();
    }

    /**
     * Stores cookies in cache
     * @param cookies Cookie string to store
     */
    public void storeCookies(String cookies) {
        cookieCache.put(NSE_COOKIE_KEY, cookies);
        log.info("Updated cookies in cache: {}", maskCookieValues(cookies));
    }

    /**
     * Retrieves cookies from cache
     * @return Cached cookies or null if not found
     */
    public String getCookies() {
        String cookies = cookieCache.getIfPresent(NSE_COOKIE_KEY);
        if (cookies != null) {
            log.debug("Retrieved cookies from cache: {}", maskCookieValues(cookies));
        }
        return cookies;
    }

    /**
     * Retrieves cookies from cache or throws exception if not found
     * @return Cached cookies
     * @throws CookieException if no cookies are found
     */
    public String getCookiesOrThrow() throws CookieException {
        String cookies = cookieCache.getIfPresent(NSE_COOKIE_KEY);
        if (cookies == null) {
            throw new CookieException("No valid cookies found in cache");
        }
        return cookies;
    }

    /**
     * Invalidates cached cookies
     */
    public void invalidateCookies() {
        String cookies = cookieCache.getIfPresent(NSE_COOKIE_KEY);
        if (cookies != null) {
            log.info("Invalidating cached cookies: {}", maskCookieValues(cookies));
            cookieCache.invalidate(NSE_COOKIE_KEY);
        }
    }

    private String maskCookieValues(String cookies) {
        if (cookies == null) return null;
        
        // Split cookies and mask values while preserving names
        return Stream.of(cookies.split(";"))
            .map(cookie -> {
                String[] parts = cookie.split("=", 2);
                return parts.length > 1 
                    ? parts[0].trim() + "=*****" 
                    : cookie.trim() + "=*****";
            })
            .collect(Collectors.joining("; "));
    }
}
