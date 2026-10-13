package com.am.marketdata.watchlist.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration for the market-data-watchlist module.
 * Enables component scanning for controllers, services, repositories, and DTOs.
 */
@Configuration
@ComponentScan(basePackages = "com.am.marketdata.watchlist")
public class WatchlistModuleConfig {
}
