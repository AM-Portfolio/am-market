package com.am.marketdata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {
                "com.am.common.investment.persistence.repository",
                "com.am.marketdata.service.repo",
                "com.am.marketdata.provider.upstox.repo",
                "com.am.marketdata.provider.zerodha.repo"
})
@Profile("!isolated")
public class MongoConfig {
}
