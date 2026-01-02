package com.am.marketdata.external.api.config;

import java.time.Duration;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.am.marketdata.external.api.client.ApiClient;
import com.am.marketdata.external.api.client.RestApiClient;
import com.am.marketdata.external.api.registry.ApiEndpointRegistry;
import com.am.marketdata.external.api.service.ApiHealthService;
import com.am.marketdata.external.api.service.ApiResponseProcessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Auto-configuration for the external API module
 */
@Configuration
@ComponentScan(basePackages = "com.am.marketdata.external.api")
@ConditionalOnProperty(name = "external.api.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ExternalApiAutoConfiguration {
    
    /**
     * Creates a RestTemplate for external API calls with custom connection settings
     * 
     * @param connectTimeoutMs Connection timeout in milliseconds
     * @param socketTimeoutMs Socket timeout in milliseconds
     * @param maxConnTotal Maximum total connections
     * @param maxConnPerRoute Maximum connections per route
     * @return RestTemplate
     */
    @Bean(name = "externalApiRestTemplate")
    @ConditionalOnMissingBean(name = "externalApiRestTemplate")
    public RestTemplate externalApiRestTemplate(
            @Value("${external.api.http.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${external.api.http.socket-timeout-ms:30000}") int socketTimeoutMs,
            @Value("${external.api.http.max-conn-total:50}") int maxConnTotal,
            @Value("${external.api.http.max-conn-per-route:20}") int maxConnPerRoute) {
        
        // Create connection manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnTotal);
        connectionManager.setDefaultMaxPerRoute(maxConnPerRoute);
        
        // Configure request timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(socketTimeoutMs))
                .build();
        
        // Build HTTP client
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        // Create request factory
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        
        log.info("Created external API RestTemplate with connectTimeout={}ms, socketTimeout={}ms, maxConnTotal={}, maxConnPerRoute={}",
                connectTimeoutMs, socketTimeoutMs, maxConnTotal, maxConnPerRoute);
        
        return new RestTemplate(requestFactory);
    }
    
    /**
     * Creates the default API client implementation
     * 
     * @param restTemplate RestTemplate for making HTTP requests
     * @param maxRetries Maximum number of retry attempts
     * @param baseDelayMs Base delay in milliseconds between retries
     * @return ApiClient
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiClient apiClient(
            RestTemplate externalApiRestTemplate,
            @Value("${external.api.retry.max-attempts:3}") int maxRetries,
            @Value("${external.api.retry.base-delay-ms:1000}") long baseDelayMs) {
        
        log.info("Creating RestApiClient with maxRetries={}, baseDelayMs={}", maxRetries, baseDelayMs);
        return new RestApiClient(externalApiRestTemplate, maxRetries, baseDelayMs);
    }
    
    /**
     * Creates the API endpoint registry
     * 
     * @return ApiEndpointRegistry
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiEndpointRegistry apiEndpointRegistry() {
        log.info("Creating ApiEndpointRegistry");
        return new ApiEndpointRegistry();
    }
    
    /**
     * Creates the API health service
     * 
     * @param apiClient API client for making health check requests
     * @param endpointRegistry Registry of API endpoints
     * @param healthCheckIntervalMs Interval between health checks in milliseconds
     * @param healthCheckEnabled Whether health checks are enabled
     * @return ApiHealthService
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiHealthService apiHealthService(
            ApiClient apiClient,
            ApiEndpointRegistry endpointRegistry,
            @Value("${external.api.health-check.interval-ms:300000}") long healthCheckIntervalMs,
            @Value("${external.api.health-check.enabled:true}") boolean healthCheckEnabled) {
        
        log.info("Creating ApiHealthService with healthCheckIntervalMs={}, healthCheckEnabled={}", 
                healthCheckIntervalMs, healthCheckEnabled);
        return new ApiHealthService(apiClient, endpointRegistry, healthCheckIntervalMs, healthCheckEnabled);
    }
    
    /**
     * Creates the API response processor
     * 
     * @param apiClient API client for making requests
     * @param endpointRegistry Registry of API endpoints
     * @param maxRetries Maximum number of retry attempts
     * @param baseDelayMs Base delay in milliseconds between retries
     * @return ApiResponseProcessor
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiResponseProcessor apiResponseProcessor(
            ApiClient apiClient,
            ApiEndpointRegistry endpointRegistry,
            @Value("${external.api.retry.max-attempts:3}") int maxRetries,
            @Value("${external.api.retry.base-delay-ms:1000}") long baseDelayMs) {
        
        log.info("Creating ApiResponseProcessor with maxRetries={}, baseDelayMs={}", maxRetries, baseDelayMs);
        return new ApiResponseProcessor(apiClient, endpointRegistry, maxRetries, baseDelayMs);
    }
}
