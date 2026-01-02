package com.am.marketdata.provider.upstox.config;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.am.marketdata.provider.upstox.UpstoxApiService;
import com.am.marketdata.provider.upstox.UpstoxIndexIdentifier;
import com.am.marketdata.provider.upstox.UpstoxMarketDataProvider;
import com.am.marketdata.provider.upstox.UpstoxSdkService;

import java.time.Duration;

/**
 * Configuration for Upstox API integration
 */
@Slf4j
@Configuration
@EnableScheduling
public class UpstoxApiConfig {

    @Value("${market-data.upstox.api.max.retries:3}")
    private int maxRetries;

    @Value("${market-data.upstox.api.retry.delay.ms:1000}")
    private int retryDelayMs;

    @Bean(name = "upstoxApiService")
    public UpstoxApiService upstoxApiService(
            com.am.marketdata.provider.upstox.client.UpStockClient upStockClient,
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            com.am.marketdata.provider.upstox.config.UpstoxConfig upstoxConfig) {
        log.info("Creating Upstox API service");
        return new UpstoxApiService(upStockClient, redisTemplate, objectMapper, upstoxConfig);
    }

    @Bean(name = "upstoxSdkService")
    public UpstoxSdkService upstoxSdkService(
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
            com.am.marketdata.provider.upstox.config.UpstoxConfig upstoxConfig) {
        log.info("Creating Upstox SDK service");
        return new UpstoxSdkService(redisTemplate, upstoxConfig);
    }

    @Bean(name = "marketDataUpstoxRetryRegistry")
    public RetryRegistry retryRegistry(MeterRegistry meterRegistry) {
        log.info("Creating Upstox retry registry with max retries: {}, base delay: {}ms",
                maxRetries, retryDelayMs);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .waitDuration(Duration.ofMillis(retryDelayMs))
                .retryExceptions(Exception.class)
                .ignoreExceptions(InterruptedException.class)
                .build();

        return RetryRegistry.of(config);
    }
}
