package com.am.marketdata.scraper.cookie;

import com.am.marketdata.scraper.config.ScraperConfig;
import com.am.marketdata.scraper.model.CookieInfo;
import com.am.marketdata.scraper.model.WebsiteCookies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles cookie scraping from NSE website
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CookieScraper {
    private final ScraperConfig scraperConfig;
    
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Scrapes cookies from NSE website
     * @return WebsiteCookies object containing all scraped cookies
     */
    public WebsiteCookies scrapeCookies() {
        return scrapeCookies(scraperConfig.getUrls().stream().findFirst().get());
    }

    /**
     * Scrapes cookies from specified URL
     * @param url URL to scrape cookies from
     * @return WebsiteCookies object containing all scraped cookies
     */
    public WebsiteCookies scrapeCookies(String url) {
        log.info("Starting to scrape cookies from URL: {}", url);
        ChromeDriver webDriver = null;
        try {
            webDriver = scraperConfig.webDriver();
            
            // Set page load timeout
            webDriver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
            
            // Clear existing cookies before navigating
            webDriver.manage().deleteAllCookies();
            log.debug("Cleared existing cookies");

            // Navigate to the URL
            log.debug("Navigating to URL: {}", url);
            webDriver.get(url);

            // Wait for page to be in ready state
            waitForPageLoad(webDriver);
            log.debug("Page loaded successfully");

            // Get cookies after page load
            Set<Cookie> cookieSet = webDriver.manage().getCookies();
            List<Cookie> seleniumCookies = new ArrayList<>(cookieSet);
            log.info("Found {} cookies for URL: {}", seleniumCookies.size(), url);

            if (seleniumCookies.isEmpty()) {
                log.warn("No cookies found for URL: {}. This might indicate blocking or incorrect page load.", url);
                return WebsiteCookies.builder()
                        .websiteUrl(url)
                        .websiteName(webDriver.getTitle())
                        .build();
            }

            // Convert to CookieInfo
            List<CookieInfo> cookies = seleniumCookies.stream()
                    .map(this::mapToCookieInfo)
                    .collect(Collectors.toList());

            // Create WebsiteCookies object
            WebsiteCookies websiteCookies = WebsiteCookies.builder()
                    .websiteUrl(url)
                    .websiteName(webDriver.getTitle())
                    .cookies(cookies)
                    .build();
            
            // Generate and set the formatted cookie string
            websiteCookies.setCookiesString(websiteCookies.generateCookiesString());
            
            log.info("Successfully scraped cookies for URL: {}", url);
            return websiteCookies;
                    
        } catch (TimeoutException e) {
            log.error("Timeout while loading URL: {}", url, e);
            throw new RuntimeException("Page load timeout for URL: " + url, e);
        } catch (Exception e) {
            log.error("Error scraping cookies for URL: {}", url, e);
            throw new RuntimeException("Error scraping cookies: " + e.getMessage(), e);
        }
    }

    private void waitForPageLoad(ChromeDriver driver) {
        new WebDriverWait(driver, WAIT_TIMEOUT)
                .until((ExpectedCondition<Boolean>) wd -> 
                        ((ChromeDriver) wd).executeScript("return document.readyState").equals("complete"));
    }

    private CookieInfo mapToCookieInfo(Cookie cookie) {
        return CookieInfo.builder()
                .name(cookie.getName())
                .value(cookie.getValue())
                .domain(cookie.getDomain())
                .path(cookie.getPath())
                .expiry(cookie.getExpiry().getTime())
                .secure(cookie.isSecure())
                .httpOnly(cookie.isHttpOnly())
                .build();
    }
}
