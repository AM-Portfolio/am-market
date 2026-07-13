package com.am.marketdata.common.config;

import com.am.logging.AMLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Value("${spring.application.name:am-market-data}")
    private String serviceName;

    @Value("${am.logging.cls-url:http://am-logging:8000/api/v1/logs}")
    private String clsUrl;

    @Bean
    public AMLogger amLogger() {
        return new AMLogger(serviceName, clsUrl);
    }
}
