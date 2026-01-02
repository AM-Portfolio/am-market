package com.am.marketdata.upstock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "upstox.auth")
public class UpstoxConfig {
    private String accessToken;
    private String baseUrl;
    private String apiKey;
} 