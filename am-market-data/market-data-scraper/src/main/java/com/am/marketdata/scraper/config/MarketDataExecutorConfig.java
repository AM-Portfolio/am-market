package com.am.marketdata.scraper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Slf4j
public class MarketDataExecutorConfig {
    private static final String THREAD_PREFIX = "market-data-";
    
    @Value("${market.data.thread.pool.size:5}")
    private int threadPoolSize;

    @Value("${market.data.thread.queue.capacity:10}")
    private int queueCapacity;

    private ThreadPoolTaskExecutor executor;

    @Bean
    public ThreadPoolTaskExecutor marketDataExecutor() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(THREAD_PREFIX);
        executor.initialize();
        
        log.info("Initialized market data thread pool with size: {}, queue capacity: {}", 
                threadPoolSize, queueCapacity);
        
        return executor;
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        if (executor != null) {
            log.info("Shutting down market data thread pool executor");
            executor.shutdown();
        }
    }
}
