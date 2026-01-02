// package com.am.marketdata.scheduler.config;

// import org.springframework.boot.autoconfigure.AutoConfiguration;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.context.annotation.ComponentScan;
// import org.springframework.context.annotation.Import;
// import org.springframework.scheduling.annotation.EnableAsync;
// import org.springframework.scheduling.annotation.EnableScheduling;

// import com.am.marketdata.processor.config.ProcessorModuleConfig;

// /**
//  * Auto-configuration for the scheduler module
//  * Enables auto-discovery and configuration of scheduler components
//  */
// @AutoConfiguration
// @EnableScheduling
// @EnableAsync
// @Import(SchedulerModuleConfig.class)
// @ComponentScan(basePackages = "com.am.marketdata.scheduler")
// @ConditionalOnProperty(value = "scheduler.module.enabled", havingValue = "true", matchIfMissing = true)
// public class SchedulerAutoConfiguration {
//     // Auto-configuration class
// }
