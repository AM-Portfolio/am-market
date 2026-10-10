package com.am.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

//import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

//import com.am.marketdata.config.ISINConfig;
//import com.am.marketdata.scraper.config.NSEIndicesConfig;
//import com.am.common.investment.persistence.config.InfluxDBConfig;
//import com.am.marketdata.external.api.config.ExternalApiAutoConfiguration;
//import com.am.common.investment.persistence.config.InfluxDBConfig;
//import com.am.marketdata.processor.config.ProcessorModuleConfig;
//import com.am.marketdata.scheduler.config.SchedulerAutoConfiguration;
import com.am.marketdata.api.config.SecurityConfig;
import com.am.marketdata.config.MetricsConfig;
import com.am.marketdata.internal.config.MarketDataInternalConfig;
import com.am.marketdata.scheduler.config.MarketDataSchedulerConfig;
import com.am.marketdata.kafka.config.MarketDataKafkaModuleConfig;

import com.am.marketdata.watchlist.config.WatchlistModuleConfig;

@SpringBootApplication(exclude = {
                DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class
})
@EnableConfigurationProperties
@Import({ MetricsConfig.class, SecurityConfig.class,
                MarketDataInternalConfig.class, MarketDataSchedulerConfig.class,
                MarketDataKafkaModuleConfig.class, WatchlistModuleConfig.class })
@ComponentScans({
                @ComponentScan(
                                basePackages = "com.am.marketdata",
                                excludeFilters = @ComponentScan.Filter(
                                                type = FilterType.REGEX,
                                                pattern = "com\\.am\\.marketdata\\.(kafka|service\\.kafka)\\..*")),
                @ComponentScan("com.marketdata")
})

// @EnableRetry
@EnableScheduling
@org.springframework.cache.annotation.EnableCaching
@org.springframework.data.mongodb.repository.config.EnableMongoRepositories(basePackages = "com.am.marketdata")
public class MarketDataApplication {
        private static final Logger logger = LoggerFactory.getLogger(MarketDataApplication.class);

        public static void main(String[] args) {
                // Log only presence. URLs and credentials must never enter application logs.
                logger.info("Market Data startup configuration: influxHostPresent={}, influxPortPresent={}, "
                                + "influxTokenPresent={}, influxOrgPresent={}, influxBucketPresent={}",
                                isPresent("INFLUXDB_HOST"), isPresent("INFLUXDB_PORT"),
                                isPresent("INFLUXDB_TOKEN"), isPresent("INFLUXDB_ORG"),
                                isPresent("INFLUXDB_BUCKET"));

                SpringApplication.run(MarketDataApplication.class, args);
        }

        private static boolean isPresent(String environmentVariable) {
                String value = System.getenv(environmentVariable);
                return value != null && !value.isBlank();
        }
}
