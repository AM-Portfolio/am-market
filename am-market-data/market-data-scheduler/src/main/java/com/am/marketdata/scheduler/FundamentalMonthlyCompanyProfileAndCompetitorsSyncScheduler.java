package com.am.marketdata.scheduler;

import com.am.marketdata.common.model.fundamental.CompanyProfile;
import com.am.marketdata.common.model.fundamental.CompetitorPeer;
import com.am.marketdata.common.model.fundamental.FundamentalData;
import com.am.marketdata.common.provider.FundamentalDataProvider;
import com.am.marketdata.provider.common.FundamentalDataProviderFactory;
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
 * Scheduled sync task for low-volatility company metadata (Profile, Sector classifications, Competitors list).
 * Runs monthly on the 1st of every month at 02:00 IST.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundamentalMonthlyCompanyProfileAndCompetitorsSyncScheduler {

    private final FundamentalDataRepository fundamentalDataRepository;
    private final FundamentalDataProviderFactory providerFactory;

    @Value("${market-data.fundamentals.scheduler.monthly.enabled:true}")
    private boolean enabled;

    /**
     * Monthly sync running on the 1st of every month at 02:00 IST.
     */
    @Scheduled(cron = "${market-data.fundamentals.scheduler.monthly.cron:0 0 2 1 * *}", zone = "Asia/Kolkata")
    @TrackedAndLockedScheduler(
            name = "syncMonthlyCompanyProfileAndCompetitors",
            lockAtMostFor = "90m",
            lockAtLeastFor = "5m")
    public void syncMonthlyCompanyProfileAndCompetitors() {
        if (!enabled) {
            log.info("Fundamental monthly sync scheduler is disabled");
            return;
        }

        log.info("Starting monthly company profile and competitors sync");
        FundamentalDataProvider provider = providerFactory.getActiveProvider();
        if (provider == null) {
            log.warn("No active FundamentalDataProvider configured, aborting monthly sync");
            return;
        }

        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        List<FundamentalData> staleDocs = fundamentalDataRepository.findBySectionStale("profile", threshold);
        log.info("Found {} stock documents requiring monthly profile and competitors refresh", staleDocs.size());

        for (FundamentalData doc : staleDocs) {
            String isin = doc.getIsin();
            if (isin == null || isin.isEmpty()) continue;

            try {
                CompanyProfile updatedProfile = provider.getCompanyProfile(isin);
                List<CompetitorPeer> updatedPeers = provider.getCompetitors(isin);

                if (updatedProfile != null) doc.setCompanyProfile(updatedProfile);
                if (updatedPeers != null) doc.setPeers(updatedPeers);

                Instant now = Instant.now();
                if (doc.getSectionLastUpdated() == null) {
                    doc.setSectionLastUpdated(new HashMap<>());
                }
                doc.getSectionLastUpdated().put("profile", now);
                doc.getSectionLastUpdated().put("competitors", now);
                doc.setLastUpdated(now);

                fundamentalDataRepository.save(doc);

                // Polite delay
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Monthly fundamental sync interrupted");
                break;
            } catch (Exception e) {
                log.error("Error refreshing monthly fundamentals for isin={}: {}", isin, e.getMessage());
            }
        }
        log.info("Completed monthly company profile and competitors sync");
    }
}
