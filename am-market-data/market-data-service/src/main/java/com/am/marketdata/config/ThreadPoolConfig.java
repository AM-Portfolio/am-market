package com.am.marketdata.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for thread pools used in the application
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${market.data.persistence.thread.pool.size:5}")
    private int persistenceThreadPoolSize;

    @Value("${market.data.persistence.thread.queue.capacity:10}")
    private int persistenceQueueCapacity;

    /**
     * Thread pool for market data persistence operations
     * @return ThreadPoolTaskExecutor configured for persistence operations
     */
    @Bean(name = "marketDataPersistenceExecutor")
    public ThreadPoolTaskExecutor marketDataPersistenceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(persistenceThreadPoolSize);
        executor.setMaxPoolSize(persistenceThreadPoolSize * 2);
        executor.setQueueCapacity(persistenceQueueCapacity);
        executor.setThreadNamePrefix("market-data-persistence-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
