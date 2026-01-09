package com.am.marketdata.internal.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ComponentScan(basePackages = "com.am.marketdata.internal")
@EnableMongoRepositories(basePackages = "com.am.marketdata.internal.repository")
@Profile("!isolated")
public class MarketDataInternalConfig {
}
