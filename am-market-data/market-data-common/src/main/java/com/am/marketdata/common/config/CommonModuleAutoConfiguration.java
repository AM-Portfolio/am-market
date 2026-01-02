package com.am.marketdata.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for common module
 */
@AutoConfiguration
@Import(CommonModuleConfig.class)
public class CommonModuleAutoConfiguration {
    
    /**
     * Constructor for auto-configuration class
     */
    public CommonModuleAutoConfiguration() {
        // Default constructor
    }
}
