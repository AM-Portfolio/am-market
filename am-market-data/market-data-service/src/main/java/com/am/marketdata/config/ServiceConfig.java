package com.am.marketdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.Executor;

/**
 * Configuration for Market Data Service
 * Enables component scanning, scheduling, and async execution
 */
@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {"com.am.marketdata.service", "com.marketdata.common", "com.am.common"})
public class ServiceConfig {
    
    /**
     * Configure async task executor for concurrent operations
     */
    @Bean(name = "marketDataTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("MarketData-");
        executor.initialize();
        return executor;
    }
}
