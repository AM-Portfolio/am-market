package com.am.marketdata.provider.upstox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Data
@Configuration
@ConfigurationProperties(prefix = "upstox")
@Profile("!isolated")
public class UpstoxConfig {
    private String accessToken;
    private String baseUrl;
    private String apiKey;
}