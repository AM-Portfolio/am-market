package com.am.marketdata.api.scheduler;

import com.am.marketdata.api.service.StockIndicesService;
import com.am.marketdata.common.log.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketDataEodSyncScheduler {

    private final AppLogger log = AppLogger.getLogger();
    private final StockIndicesService stockIndicesService;

    // Major Indian Indices to sync at EOD
    private static final List<String> MAJOR_INDICES = Arrays.asList(
            "NIFTY 50", "NIFTY BANK", "NIFTY FIN SERVICE", "NIFTY IT", 
            "NIFTY 100", "NIFTY 200", "NIFTY 500", "NIFTY MIDCAP 100", 
            "NIFTY SMLCAP 100"
    );

    /**
     * Runs Monday to Friday at 3:40 PM IST (15:40) to sync final EOD prices 
     * from Redis back to MongoDB for permanent storage.
     * 
     * Cron expression: "0 40 15 * * MON-FRI"
     * TimeZone: "Asia/Kolkata"
     */
    @Scheduled(cron = "0 40 15 * * MON-FRI", zone = "Asia/Kolkata")
    @com.am.scheduler.annotation.TrackedAndLockedScheduler(name = "marketEodSyncJob", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void syncEodIndexPrices() {
        String methodName = "syncEodIndexPrices";
        log.info(methodName, "Starting EOD Index Price Sync Scheduler to persist Redis prices to MongoDB");

        try {
            // Force a refresh (forceRefresh = true) to fetch the official exchange-adjusted 
            // post-market closing prices from Upstox instead of copying the raw 3:30 PM ticks from Redis.
            stockIndicesService.getLatestIndicesData(MAJOR_INDICES, true);
            log.info(methodName, "Successfully completed EOD Index Price Sync");
        } catch (Exception e) {
            log.error(methodName, "Error during EOD Index Price Sync", e);
        }
    }
}
