package com.am.common.investment.persistence.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDBConfig {

    @Value("${spring.influx.url}")
    private String url;

    @Value("${spring.influx.token}")
    private String token;

    @Value("${spring.influx.org}")
    private String org;

    @Value("${spring.influx.bucket}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        String effectiveUrl = resolveEffectiveUrl(url);
        return InfluxDBClientFactory.create(effectiveUrl, token.toCharArray(), org, bucket);
    }
    
    public String getBucket() {
        return bucket;
    }
    
    public String getOrg() {
        return org;
    }

    private String resolveEffectiveUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("http://") && (trimmed.endsWith(":443") || trimmed.contains(":443/"))) {
            return "https://" + trimmed.substring("http://".length()).replace(":443", "");
        }
        return trimmed;
    }
}
