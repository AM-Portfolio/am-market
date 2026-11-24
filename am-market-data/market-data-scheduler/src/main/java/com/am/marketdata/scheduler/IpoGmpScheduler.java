package com.am.marketdata.scheduler;

import com.am.marketdata.service.ipo.IpoGmpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoGmpScheduler {
    private final IpoGmpService ipoGmpService;

    @Value("${market-data.ipo.gmp.enabled:true}") private boolean enabled;

    @Scheduled(cron = "${scheduler.ipo.gmp-cron:0 15 0 * * *}", zone = "Asia/Kolkata")
    @com.am.scheduler.annotation.TrackedAndLockedScheduler(name = "ipoGmpRefreshSchedulerJob", lockAtMostFor = "20m", lockAtLeastFor = "1m")
    public void refresh() {
        if (enabled) log.info("IPO GMP refresh completed; updated {} IPOs", ipoGmpService.refresh());
    }
}
