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
    public void syncEodIndexPrices() {
        String methodName = "syncEodIndexPrices";
        log.info(methodName, "Starting EOD Index Price Sync Scheduler to persist Redis prices to MongoDB");

        try {
            // Calling getLatestIndicesData automatically retrieves Redis prices and 
            // debounces/saves them to MongoDB if prices are available.
            stockIndicesService.getLatestIndicesData(MAJOR_INDICES, false);
            log.info(methodName, "Successfully completed EOD Index Price Sync");
        } catch (Exception e) {
            log.error(methodName, "Error during EOD Index Price Sync", e);
        }
    }
}
