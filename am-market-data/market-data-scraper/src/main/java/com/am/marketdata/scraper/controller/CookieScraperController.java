package com.am.marketdata.scraper.controller;

import com.am.marketdata.scraper.config.ScraperConfig;
import com.am.marketdata.scraper.cookie.CookieCache;
import com.am.marketdata.scraper.cookie.CookieScraper;
import com.am.marketdata.scraper.model.WebsiteCookies;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
public class CookieScraperController {
    private final CookieScraper scraperService;
    private final ScraperConfig scraperConfig;
    private final CookieCache cookieCache;

    @GetMapping("/cookies")
    public List<WebsiteCookies> scrapeCookies() {
        List<WebsiteCookies> results = scraperConfig.getUrls().stream()
                .map(scraperService::scrapeCookies)
                .collect(Collectors.toList());

        // Store cookies in cache
        results.forEach(cookies -> {
            if (cookies.getCookiesString() != null && !cookies.getCookiesString().isEmpty()) {
                cookieCache.storeCookies(cookies.getCookiesString());
            }
        });

        return results;
    }
}
