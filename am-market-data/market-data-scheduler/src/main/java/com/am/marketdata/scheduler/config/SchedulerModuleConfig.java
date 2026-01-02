package com.am.marketdata.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration class for the scheduler module
 */
@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {
    "com.am.marketdata.scheduler.service",
    "com.am.marketdata.scheduler.config",
    "com.am.marketdata.processor.service",
    "com.am.marketdata.service"
})
public class SchedulerModuleConfig {

    /**
     * Task scheduler for scheduled operations
     * 
     * @return ThreadPoolTaskScheduler
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("market-data-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
}
