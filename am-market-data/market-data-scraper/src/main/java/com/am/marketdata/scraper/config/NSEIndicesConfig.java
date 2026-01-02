package com.am.marketdata.scraper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Data
@Configuration
@Primary
@ConfigurationProperties(prefix = "nse")
public class NSEIndicesConfig {
    private List<String> broadMarketIndices;
    private List<String> sectorIndices;
    private String baseUrl;
    private Api api;
    private Headers headers;

    @Data
    public static class Api {
        private String indices;
        private String stockIndices;
    }

    @Data
    public static class Headers {
        private String userAgent;
        private String accept;
        private String acceptLanguage;
    }
}
