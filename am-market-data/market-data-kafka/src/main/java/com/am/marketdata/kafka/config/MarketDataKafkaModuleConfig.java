package com.am.marketdata.kafka.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = {
    "com.am.marketdata.kafka",
    "com.am.marketdata.service.kafka",
    "com.am.marketdata.common",
    "com.am.marketdata.external",
    "com.am.marketdata.kafka.config"
})
@Import(KafkaConfig.class)
public class MarketDataKafkaModuleConfig {
    
}
