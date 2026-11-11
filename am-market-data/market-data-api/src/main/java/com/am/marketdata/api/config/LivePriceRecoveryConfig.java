package com.am.marketdata.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bounded executor for provider recovery on interactive index requests.
 * Provider I/O must not consume the JVM-wide ForkJoinPool.
 */
@Configuration
public class LivePriceRecoveryConfig {

    @Bean(name = "livePriceRecoveryExecutor")
    public ThreadPoolTaskExecutor livePriceRecoveryExecutor(
            @Value("${market.data.live-price-recovery.core-pool-size:4}") int corePoolSize,
            @Value("${market.data.live-price-recovery.max-pool-size:8}") int maxPoolSize,
            @Value("${market.data.live-price-recovery.queue-capacity:32}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("live-price-recovery-");
        // Apply backpressure instead of silently dropping a recovery request.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
