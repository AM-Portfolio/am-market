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

    /** Runs independently after the nightly IPO refresh window. */
    @Scheduled(cron = "${scheduler.ipo.gmp-cron:0 15 0 * * *}", zone = "Asia/Kolkata")
    @com.am.scheduler.annotation.TrackedAndLockedScheduler(name = "ipoGmpRefreshSchedulerJob", lockAtMostFor = "20m", lockAtLeastFor = "1m")
    public void refresh() {
        if (!enabled) { log.info("IPO GMP scheduler skipped: disabled"); return; }
        var result = ipoGmpService.refresh();
        if ("FAILED".equals(result.getStatus())) {
            log.error("IPO GMP refresh failed: error={}, received={}, matched={}, updated={}, duplicates={}, unmatched={}, skipped={}",
                    result.getError(), result.getReceived(), result.getMatched(), result.getUpdated(), result.getDuplicates(), result.getUnmatched(), result.getSkipped());
        } else {
            log.info("IPO GMP refresh {}: received={}, matched={}, updated={}, duplicates={}, unmatched={}, skipped={}",
                    result.getStatus(), result.getReceived(), result.getMatched(), result.getUpdated(), result.getDuplicates(), result.getUnmatched(), result.getSkipped());
        }
    }
}
