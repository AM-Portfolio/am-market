package com.am.marketdata.scraper.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CookieInfo {
    private String name;
    private String value;
    private String domain;
    private String path;
    private Boolean secure;
    private Boolean httpOnly;
    private String sameSite;
    private Long expiry;
}
