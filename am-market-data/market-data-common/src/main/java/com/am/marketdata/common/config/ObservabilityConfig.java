package com.am.marketdata.common.config;

import com.am.marketdata.common.observability.FlowLogger;
import com.am.marketdata.common.observability.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public FlowLogger flowLogger() {
        return new FlowLogger();
    }

    @Bean
    public MdcTaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
