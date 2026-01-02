package com.am.common.investment.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.am.common.investment.persistence.config.InfluxDBConfig;

@SpringBootApplication
@Import(InfluxDBConfig.class)
public class InvestmentDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvestmentDataApplication.class, args);
    }
}