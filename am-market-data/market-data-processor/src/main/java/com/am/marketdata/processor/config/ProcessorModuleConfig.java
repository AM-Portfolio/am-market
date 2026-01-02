package com.am.marketdata.processor.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.am.marketdata.kafka.config.MarketDataKafkaModuleConfig;

import lombok.RequiredArgsConstructor;

/**
 * Configuration for the processor module
 */
@Configuration
@EnableAsync
@ComponentScan(basePackages = {
    "com.am.marketdata.processor.service",
    "com.am.marketdata.processor.factory",
    "com.am.marketdata.processor.service.mapper"
})
@Import(MarketDataKafkaModuleConfig.class)
@RequiredArgsConstructor
public class ProcessorModuleConfig {

    /**
     * Configure the async executor for the processor module
     * @return Executor
     */
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("MarketData-");
        executor.initialize();
        return executor;
    }

}
