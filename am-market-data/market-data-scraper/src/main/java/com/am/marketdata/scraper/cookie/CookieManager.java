package com.am.marketdata.scraper.cookie;

import com.am.marketdata.scraper.model.WebsiteCookies;
import com.am.marketdata.scraper.exception.CookieException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieManager {
    private final CookieScraper cookieScraper;
    private final CookieValidator cookieValidator;
    private final CookieCache cookieCache;
    private final ReentrantLock refreshLock = new ReentrantLock();

    public WebsiteCookies fetchAndValidateCookies() throws CookieException {
        try {
            WebsiteCookies websiteCookies = cookieScraper.scrapeCookies();
            String cookieString = websiteCookies.getCookiesString();
            if (cookieString == null || cookieString.isBlank()) {
                throw new CookieException("Selenium scrape returned empty cookie string");
            }

            Map<String, CookieValidator.ValidationResult> validationResults =
                    cookieValidator.validateAllCookies(cookieString);

            boolean isValid = true;
            for (String requiredCookie : cookieValidator.getRequiredCookies()) {
                CookieValidator.ValidationResult result = validationResults.get(requiredCookie);
                if (result == null || !result.isValid()) {
                    log.warn("Required cookie {} is invalid: {}", requiredCookie,
                            result == null ? "not found" : result.getMessage());
                    isValid = false;
                }
            }

            if (!isValid) {
                throw new CookieException("Cookie validation failed: Required cookies are invalid or missing");
            }

            return websiteCookies;
        } catch (CookieException e) {
            throw e;
        } catch (Exception e) {
            throw new CookieException("Failed to fetch and validate cookies: " + e.getMessage(), e);
        }
    }

    /**
     * Writer path: scrape via Selenium under lock and store in Redis + L1.
     */
    public String refreshFromSelenium() throws CookieException {
        refreshLock.lock();
        try {
            log.info("Refreshing NSE cookies via Selenium (writer)");
            WebsiteCookies newCookies = fetchAndValidateCookies();
            cookieCache.storeCookies(newCookies.getCookiesString());
            return newCookies.getCookiesString();
        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * Writer path: scrape only when Redis/L1 is empty or required cookies look invalid.
     */
    public boolean refreshIfNeeded() {
        try {
            String currentCookies = cookieCache.getCookies();
            if (currentCookies == null
                    || !cookieValidator.areRequiredCookiesValid(currentCookies)
                    || cookieValidator.areAnyRequiredCookiesExpiringSoon(currentCookies, 10)) {
                refreshFromSelenium();
                return true;
            }
            return false;
        } catch (CookieException e) {
            log.error("Cookie refresh failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reader path for NSE API / IPO sync: Redis (and L1) only. Never launches Selenium.
     */
    public String getCookiesForApi() throws CookieException {
        return cookieCache.getCookiesOrThrow();
    }

    /**
     * @deprecated Prefer {@link #getCookiesForApi()} for readers and {@link #refreshFromSelenium()} for writers.
     * Kept for callers; now Redis-only (no auto-scrape).
     */
    public String getValidCookies() throws CookieException {
        return getCookiesForApi();
    }

    public CookieCache.CookiePresenceStatus setCookiesFromHeader(String cookieHeader, Integer ttlMinutes) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            throw new CookieException("cookieHeader must not be blank");
        }
        String trimmed = cookieHeader.trim();
        cookieValidator.validateAllCookies(trimmed);
        int ttl = ttlMinutes == null ? cookieCache.getTtlMinutes() : ttlMinutes;
        cookieCache.storeCookies(trimmed, ttl);
        return cookieCache.status();
    }

    public CookieCache.CookiePresenceStatus status() {
        return cookieCache.status();
    }

    public void invalidate() {
        cookieCache.invalidateCookies();
    }
}
