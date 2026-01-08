package com.am.marketdata.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.am.marketdata.provider.zerodha.config.ZerodhaApiConfig;

@Configuration
@ComponentScan(basePackages = {
                "com.am.marketdata.api", // External API module
                "com.am.marketdata.common", // Common API module
                "com.am.marketdata.service", // Services
                "com.am.marketdata.tradebrain"
})
@Import({
                ZerodhaApiConfig.class
})
public class MarketDataApiModuleConfig {

}
