package com.am.marketdata.scheduler.registry;

import com.am.marketdata.scheduler.service.AbstractMarketDataScheduler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all market data schedulers
 * Provides a central place to manage and access all schedulers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerRegistry {

    private final ApplicationContext applicationContext;
    private final Map<String, AbstractMarketDataScheduler<?>> schedulers = new HashMap<>();

    /**
     * Initialize the registry by finding all scheduler beans
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing SchedulerRegistry");
        
        // Find all scheduler beans
        Map<String, AbstractMarketDataScheduler> schedulerBeans = 
            applicationContext.getBeansOfType(AbstractMarketDataScheduler.class);
        
        // Register each scheduler
        schedulerBeans.forEach((name, scheduler) -> {
            String schedulerName = scheduler.getSchedulerName();
            log.info("Registering scheduler: {} ({})", schedulerName, name);
            schedulers.put(schedulerName, scheduler);
        });
        
        log.info("Registered {} schedulers", schedulers.size());
    }

    /**
     * Get a scheduler by name
     * 
     * @param name The name of the scheduler
     * @return The scheduler, or null if not found
     */
    public AbstractMarketDataScheduler<?> getScheduler(String name) {
        return schedulers.get(name);
    }

    /**
     * Get all registered schedulers
     * 
     * @return Collection of all schedulers
     */
    public Collection<AbstractMarketDataScheduler<?>> getAllSchedulers() {
        return schedulers.values();
    }

    /**
     * Register a new scheduler
     * 
     * @param scheduler The scheduler to register
     */
    public void registerScheduler(AbstractMarketDataScheduler<?> scheduler) {
        String schedulerName = scheduler.getSchedulerName();
        log.info("Registering scheduler: {}", schedulerName);
        schedulers.put(schedulerName, scheduler);
    }

    /**
     * Unregister a scheduler
     * 
     * @param name The name of the scheduler to unregister
     * @return The unregistered scheduler, or null if not found
     */
    public AbstractMarketDataScheduler<?> unregisterScheduler(String name) {
        log.info("Unregistering scheduler: {}", name);
        return schedulers.remove(name);
    }
}
