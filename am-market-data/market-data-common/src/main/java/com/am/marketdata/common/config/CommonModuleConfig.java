package com.am.marketdata.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for common module dependencies
 */
@Configuration
@ComponentScans({
    @ComponentScan(basePackages = "com.am.marketdata.common.model.stockdetails"),
    @ComponentScan(basePackages = "com.am.marketdata.common.model.events")
})
public class CommonModuleConfig {
    
    /**
     * Constructor for configuration class
     */
    public CommonModuleConfig() {
        // Default constructor
    }
}
