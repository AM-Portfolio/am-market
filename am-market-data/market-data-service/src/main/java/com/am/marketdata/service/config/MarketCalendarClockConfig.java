package com.am.marketdata.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MarketCalendarClockConfig {

    @Bean
    public Clock marketCalendarClock() {
        return Clock.system(java.time.ZoneId.of("Asia/Kolkata"));
    }
}
