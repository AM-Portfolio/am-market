package com.am.marketdata.scraper.cookie;

import com.am.marketdata.scraper.model.WebsiteCookies;
import com.am.marketdata.scraper.exception.CookieException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Manages the complete cookie lifecycle including:
 * - Scraping
 * - Validation
 * - Caching
 * - Refresh
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CookieManager {
    private final CookieScraper cookieScraper;
    private final CookieValidator cookieValidator;
    private final CookieCache cookieCache;

    @Value("${app.cookie.validator.expiry-threshold-minutes:10}")
    private int expiryThresholdMinutes;
    
    /**
     * Fetches and validates fresh cookies
     * @return Validated WebsiteCookies
     * @throws CookieException if cookie validation fails
     */
    public WebsiteCookies fetchAndValidateCookies() throws CookieException {
        try {
            WebsiteCookies websiteCookies = cookieScraper.scrapeCookies();
            String cookieString = websiteCookies.getCookiesString();
            
            // Validate cookies
            Map<String, CookieValidator.ValidationResult> validationResults = 
                cookieValidator.validateAllCookies(cookieString);
            
            // Check if all required cookies are valid
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
        } catch (Exception e) {
            throw new CookieException("Failed to fetch and validate cookies: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes cookies if needed
     * @return true if cookies were refreshed
     */
    public boolean refreshIfNeeded() {
        try {
            String currentCookies = cookieCache.getCookies();
            
            if (currentCookies == null || 
                !cookieValidator.areRequiredCookiesValid(currentCookies) ||
                cookieValidator.areAnyRequiredCookiesExpiringSoon(currentCookies, 10)) {
                
                WebsiteCookies newCookies = fetchAndValidateCookies();
                cookieCache.storeCookies(newCookies.getCookiesString());
                return true;
            }
            
            return false;
        } catch (CookieException e) {
            log.error("Cookie refresh failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets valid cookies from cache or refreshes if needed
     * @return Valid cookies string
     * @throws CookieException if no valid cookies can be obtained
     */
    public String getValidCookies() throws CookieException {
        String cookies = cookieCache.getCookies();
        if (cookies == null || 
            !cookieValidator.areRequiredCookiesValid(cookies) ||
            cookieValidator.areAnyRequiredCookiesExpiringSoon(cookies, 10)) {
            
            WebsiteCookies newCookies = fetchAndValidateCookies();
            cookieCache.storeCookies(newCookies.getCookiesString());
            return newCookies.getCookiesString();
        }
        
        return cookies;
    }
}
