package com.am.marketdata.scheduler;

import com.am.marketdata.service.calendar.MarketCalendarSyncService;
import com.am.marketdata.service.calendar.MarketCalendarSyncTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketCalendarSyncScheduler {

    private final MarketCalendarSyncService marketCalendarSyncService;

    @Value("${market-data.calendar.exchange:NSE}")
    private String exchange;

    @Value("${market-data.calendar.sync.enabled:true}")
    private boolean syncEnabled;

    @Scheduled(cron = "${scheduler.market-calendar.sync-cron:0 30 6 * * *}", zone = "Asia/Kolkata")
    @com.am.scheduler.annotation.TrackedAndLockedScheduler(
            name = "marketCalendarSync",
            lockAtMostFor = "15m",
            lockAtLeastFor = "1m")
    public void nightlySync() {
        if (!syncEnabled) {
            return;
        }
        try {
            marketCalendarSyncService.sync(exchange, MarketCalendarSyncTrigger.SCHEDULER);
        } catch (Exception e) {
            log.error("Nightly market calendar sync failed for {}", exchange, e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncIfEmptyOnStartup() {
        if (!syncEnabled) {
            return;
        }
        int year = java.time.Year.now(java.time.ZoneId.of("Asia/Kolkata")).getValue();
        if (marketCalendarSyncService.hasYearData(exchange, year)) {
            return;
        }
        new Thread(() -> {
            try {
                log.info("Startup market calendar sync (empty DB) exchange={}", exchange);
                marketCalendarSyncService.sync(exchange, MarketCalendarSyncTrigger.STARTUP);
            } catch (Exception e) {
                log.warn("Startup market calendar sync failed: {}", e.getMessage());
            }
        }, "market-calendar-startup-sync").start();
    }
}
