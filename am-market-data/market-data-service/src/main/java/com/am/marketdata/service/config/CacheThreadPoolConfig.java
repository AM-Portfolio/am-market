package com.am.marketdata.service.config;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.*;

/**
 * DEDICATED THREAD POOL CONFIGURATION FOR ASYNC CACHE BACKFILLING
 * ---------------------------------------------------------------------------------------------
 * WHAT PROBLEM THIS SOLVES:
 * Prevents background Redis cache writing from blocking HTTP request threads or consuming
 * the shared Java ForkJoinPool.
 * 
 * FAILSAFE MECHANISM:
 * - Uses a bounded ArrayBlockingQueue to prevent out-of-memory errors under extreme load.
 * - Employs DiscardPolicy so if the queue overflows, non-critical background writes are
 *   silently dropped without crashing the application or throwing HTTP errors to clients.
 * - Copies MDC logging context (trace IDs) across thread boundaries so logs stay traceable.
 */
@Configuration
public class CacheThreadPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheThreadPoolConfig.class);

    @Value("${market.cache.async-backfill.core-pool-size:5}")
    private int corePoolSize;

    @Value("${market.cache.async-backfill.max-pool-size:15}")
    private int maxPoolSize;

    @Value("${market.cache.async-backfill.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "cacheBackfillExecutor")
    public Executor cacheBackfillExecutor() {
        log.info("[CACHE_THREAD_POOL] Initializing dedicated Executor: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);

        // Custom ThreadFactory to give meaningful worker names in logs/profilers (e.g. cache-backfill-1)
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "cache-backfill-" + count++);
                thread.setDaemon(true); // Daemon thread so JVM shuts down cleanly
                return thread;
            }
        };

        // Bounded ThreadPoolExecutor with DiscardPolicy (never block main API requests)
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.DiscardPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        log.warn("[CACHE_THREAD_POOL] Queue full (capacity={}). Dropping non-critical background cache write task.", queueCapacity);
                    }
                }
        );

        // Wrap execution to propagate MDC trace context (e.g. traceId, spanId) across threads
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            executor.execute(() -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            });
        };
    }
}
