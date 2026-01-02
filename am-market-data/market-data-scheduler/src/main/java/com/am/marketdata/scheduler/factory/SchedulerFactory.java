package com.am.marketdata.scheduler.factory;

import com.am.marketdata.scheduler.service.AbstractMarketDataScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory for creating and managing market data schedulers
 * Provides a central point for scheduler creation and configuration
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerFactory {

    private final ApplicationContext applicationContext;
    private final MeterRegistry meterRegistry;

    /**
     * Create a new scheduler for the given operation type
     * 
     * @param <T> The type of scheduler to create
     * @param schedulerClass The class of the scheduler to create
     * @return The created scheduler instance
     */
    public <T extends AbstractMarketDataScheduler<?>> T createScheduler(Class<T> schedulerClass) {
        log.info("Creating scheduler of type: {}", schedulerClass.getSimpleName());
        return applicationContext.getBean(schedulerClass);
    }

    /**
     * Register a new scheduler with the application context
     * This is useful for programmatically creating schedulers at runtime
     * 
     * @param scheduler The scheduler to register
     */
    public void registerScheduler(AbstractMarketDataScheduler<?> scheduler) {
        log.info("Registering scheduler: {}", scheduler.getClass().getSimpleName());
        // In a real implementation, this would register the scheduler with Spring
        // For now, we'll just log it
    }
}
