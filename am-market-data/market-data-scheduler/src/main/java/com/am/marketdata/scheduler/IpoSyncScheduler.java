package com.am.marketdata.scheduler;

import com.am.marketdata.common.ipo.IpoFeedScope;
import com.am.marketdata.service.ipo.IpoSyncService;
import com.am.marketdata.service.ipo.IpoSyncTrigger;
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
public class IpoSyncScheduler {

    private final IpoSyncService ipoSyncService;

    @Value("${market-data.ipo.enabled:true}")
    private boolean enabled;

    @Value("${market-data.ipo.sync.past-on-startup-if-empty:true}")
    private boolean pastOnStartupIfEmpty;

    @Scheduled(cron = "${scheduler.ipo.current-upcoming-cron:0 0 */3 * * *}", zone = "Asia/Kolkata")
    @com.am.scheduler.annotation.TrackedAndLockedScheduler(
            name = "ipoCurrentUpcomingSync",
            lockAtMostFor = "20m",
            lockAtLeastFor = "1m")
    public void syncCurrentUpcomingAndSubscriptions() {
        if (!enabled || !ipoSyncService.isSourceAvailable()) {
            return;
        }
        try {
            ipoSyncService.sync(IpoFeedScope.CURRENT, IpoSyncTrigger.SCHEDULER);
            ipoSyncService.sync(IpoFeedScope.UPCOMING, IpoSyncTrigger.SCHEDULER);
            ipoSyncService.sync(IpoFeedScope.SUBSCRIPTION, IpoSyncTrigger.SCHEDULER);
        } catch (Exception e) {
            log.error("IPO scheduled sync failed", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncPastIfEmptyOnStartup() {
        if (!enabled || !pastOnStartupIfEmpty || !ipoSyncService.isSourceAvailable()) {
            return;
        }
        if (ipoSyncService.hasPastData()) {
            return;
        }
        new Thread(() -> {
            try {
                log.info("Startup IPO past sync (empty DB)");
                ipoSyncService.sync(IpoFeedScope.PAST, IpoSyncTrigger.STARTUP);
            } catch (Exception e) {
                log.warn("Startup IPO past sync failed: {}", e.getMessage());
            }
        }, "ipo-past-startup-sync").start();
    }
}
