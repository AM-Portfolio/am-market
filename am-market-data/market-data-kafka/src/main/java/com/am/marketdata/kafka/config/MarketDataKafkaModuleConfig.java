package com.am.marketdata.kafka.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
    "com.am.marketdata.kafka",
    "com.am.marketdata.common",
    "com.am.marketdata.external",
    "com.am.marketdata.kafka.config"
})
@Import(KafkaConfig.class)
public class MarketDataKafkaModuleConfig {
    
}
