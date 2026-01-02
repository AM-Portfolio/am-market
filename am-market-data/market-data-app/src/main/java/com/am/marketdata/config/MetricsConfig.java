package com.am.marketdata.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for metrics and monitoring
 */
@Configuration
public class MetricsConfig {

    /**
     * Creates a simple meter registry when no other registry is available
     * This ensures that services requiring a MeterRegistry bean can be autowired properly
     * @return A simple meter registry instance
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
