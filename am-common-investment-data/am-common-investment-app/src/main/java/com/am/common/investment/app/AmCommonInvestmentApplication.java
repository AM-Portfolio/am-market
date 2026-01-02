package com.am.common.investment.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import com.am.common.investment.persistence.config.InfluxDBConfig;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = {
    "com.am.common.investment.service",
    "com.am.common.investment.persistence",
    "com.am.common.investment.persistence.document",
    "com.am.common.investment.persistence.repository",
    "com.am.common.investment.model.events.mapper",
    "com.am.common.investment.app",
    "com.am.common.investment.model",
    "com.am.common.investment.app.service"
})
@Import(InfluxDBConfig.class)
public class AmCommonInvestmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmCommonInvestmentApplication.class, args);
    }
}
