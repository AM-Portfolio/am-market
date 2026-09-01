package com.am.marketdata.scheduler;

import com.am.marketdata.common.model.fundamental.CorporateActionEntry;
import com.am.marketdata.common.model.fundamental.FundamentalData;
import com.am.marketdata.common.model.fundamental.KeyRatios;
import com.am.marketdata.common.provider.FundamentalDataProvider;
import com.am.marketdata.provider.common.FundamentalDataProviderFactory;
import com.am.marketdata.service.MarketHoursService;
import com.am.marketdata.service.repo.FundamentalDataRepository;
import com.am.scheduler.annotation.TrackedAndLockedScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

/**
 * Scheduled sync task for daily market-sensitive fundamental metrics (Key Ratios, Corporate Actions).
 * Executes post-market close (18:30 IST) strictly on valid cash trading session days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundamentalDailyKeyRatiosAndCorporateActionsSyncScheduler {

    private final FundamentalDataRepository fundamentalDataRepository;
    private final FundamentalDataProviderFactory providerFactory;
    private final MarketHoursService marketHoursService;

    @Value("${market-data.fundamentals.scheduler.daily.enabled:true}")
    private boolean enabled;

    /**
     * Daily sync running at 18:30 IST on trading days.
     * Uses ShedLock (@TrackedAndLockedScheduler) to ensure single pod execution.
     */
    @Scheduled(cron = "${market-data.fundamentals.scheduler.daily.cron:0 30 18 * * MON-FRI}", zone = "Asia/Kolkata")
    @TrackedAndLockedScheduler(
            name = "syncDailyKeyRatiosAndCorporateActions",
            lockAtMostFor = "45m",
            lockAtLeastFor = "2m")
    public void syncDailyKeyRatiosAndCorporateActions() {
        if (!enabled) {
            log.info("Fundamental daily sync scheduler is disabled");
            return;
        }

        // Verify that today is a valid cash trading session day (skips national holidays / unexpected closures)
        if (marketHoursService != null && !marketHoursService.isCashSessionDay()) {
            log.info("Skipping daily fundamental sync: today is not a cash trading session day");
            return;
        }

        log.info("Starting daily fundamental key ratios and corporate actions sync");
        FundamentalDataProvider provider = providerFactory.getActiveProvider();
        if (provider == null) {
            log.warn("No active FundamentalDataProvider configured, aborting daily sync");
            return;
        }

        Instant threshold = Instant.now().minus(20, ChronoUnit.HOURS);
        List<FundamentalData> staleDocs = fundamentalDataRepository.findBySectionStale("keyRatios", threshold);
        log.info("Found {} stock documents requiring daily key ratios refresh", staleDocs.size());

        for (FundamentalData doc : staleDocs) {
            String isin = doc.getIsin();
            if (isin == null || isin.isEmpty()) continue;

            try {
                KeyRatios updatedRatios = provider.getKeyRatios(isin);
                List<CorporateActionEntry> updatedActions = provider.getCorporateActions(isin);

                if (updatedRatios != null) {
                    doc.setKeyRatios(updatedRatios);
                }
                if (updatedActions != null) {
                    doc.setCorporateActions(updatedActions);
                }

                Instant now = Instant.now();
                if (doc.getSectionLastUpdated() == null) {
                    doc.setSectionLastUpdated(new HashMap<>());
                }
                doc.getSectionLastUpdated().put("keyRatios", now);
                doc.getSectionLastUpdated().put("corporateActions", now);
                doc.setUpdatedAt(now);
                doc.setLastUpdated(now);

                fundamentalDataRepository.save(doc);

                // Polite 500ms delay between stocks to respect broker API rate limits
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Daily fundamental sync interrupted");
                break;
            } catch (Exception e) {
                log.error("Error refreshing daily fundamentals for isin={}: {}", isin, e.getMessage());
            }
        }
        log.info("Completed daily fundamental key ratios and corporate actions sync");
    }
}
