package com.am.marketdata.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import lombok.RequiredArgsConstructor;

/**
 * Configuration for thread pools used in the application
 */
@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfig {

    private final TaskDecorator taskDecorator;

    // Increased pool size from 5 to 15 to handle high-frequency streaming database/cache writes concurrently
    @Value("${market.data.persistence.thread.pool.size:15}")
    private int persistenceThreadPoolSize;

    // Increased queue capacity from 10 to 1000 to act as a buffer during traffic bursts, preventing RejectedExecutionException
    @Value("${market.data.persistence.thread.queue.capacity:1000}")
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
        executor.setTaskDecorator(taskDecorator);
        executor.initialize();
        return executor;
    }
}
