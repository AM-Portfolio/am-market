package com.am.marketdata.scraper.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class WebsiteCookies {
    private String websiteUrl;
    private String websiteName;
    private List<CookieInfo> cookies;
    private String cookiesString;

    public String generateCookiesString() {
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }
        return cookies.stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
    }
}
