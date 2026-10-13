package com.am.marketdata.scheduler;

import com.am.marketdata.common.model.fundamental.*;
import com.am.marketdata.common.provider.FundamentalDataProvider;
import com.am.marketdata.provider.common.FundamentalDataProviderFactory;
import com.am.marketdata.service.fundamental.FundamentalCalculationEngine;
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
 * Scheduled sync task for quarterly financial statements (P&L, Balance Sheet, Cash Flow) and shareholdings.
 * Runs on a low-frequency cadence (Sunday 03:00 IST) to capture new reporting period filings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundamentalQuarterlyFinancialStatementsAndShareholdingSyncScheduler {

    private final FundamentalDataRepository fundamentalDataRepository;
    private final FundamentalDataProviderFactory providerFactory;
    private final FundamentalCalculationEngine calculationEngine;

    @Value("${market-data.fundamentals.scheduler.quarterly.enabled:true}")
    private boolean enabled;

    /**
     * Quarterly / weekly cadence sync running on Sunday 03:00 IST.
     */
    @Scheduled(cron = "${market-data.fundamentals.scheduler.quarterly.cron:0 0 3 * * SUN}", zone = "Asia/Kolkata")
    @TrackedAndLockedScheduler(
            name = "syncQuarterlyFinancialStatementsAndShareholdings",
            lockAtMostFor = "90m",
            lockAtLeastFor = "5m")
    public void syncQuarterlyFinancialStatementsAndShareholdings() {
        if (!enabled) {
            log.info("Fundamental quarterly statements sync scheduler is disabled");
            return;
        }

        log.info("Starting quarterly financial statements and shareholdings sync");
        FundamentalDataProvider provider = providerFactory.getActiveProvider();
        if (provider == null) {
            log.warn("No active FundamentalDataProvider configured, aborting quarterly sync");
            return;
        }

        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        List<FundamentalData> staleDocs = fundamentalDataRepository.findBySectionStale("incomeStatement", threshold);
        log.info("Found {} stock documents requiring financial statements refresh", staleDocs.size());

        for (FundamentalData doc : staleDocs) {
            String isin = doc.getIsin();
            if (isin == null || isin.isEmpty()) continue;

            try {
                List<IncomeStatementEntry> income = provider.getIncomeStatement(isin, true, "yearly");
                List<BalanceSheetEntry> balance = provider.getBalanceSheet(isin, true);
                List<CashFlowEntry> cashFlow = provider.getCashFlow(isin, true);
                List<ShareholdingQuarterEntry> shareholding = provider.getShareHoldings(isin);

                if (income != null && !income.isEmpty()) doc.setIncomeStatements(income);
                if (balance != null && !balance.isEmpty()) doc.setBalanceSheets(balance);
                if (cashFlow != null && !cashFlow.isEmpty()) doc.setCashFlows(cashFlow);
                if (shareholding != null && !shareholding.isEmpty()) doc.setShareholdings(shareholding);

                // Recalculate health metrics with updated statements
                FundamentalAnalytics updatedAnalytics = calculationEngine.computeAnalytics(
                        doc.getIncomeStatements(), doc.getBalanceSheets(), doc.getCashFlows(),
                        doc.getAnalytics() != null ? doc.getAnalytics().getWeek52High() : null,
                        doc.getAnalytics() != null ? doc.getAnalytics().getWeek52Low() : null,
                        doc.getAnalytics() != null ? doc.getAnalytics().getPriceCagr1Y() : null,
                        doc.getAnalytics() != null ? doc.getAnalytics().getPriceCagr3Y() : null,
                        doc.getAnalytics() != null ? doc.getAnalytics().getPriceCagr5Y() : null
                );
                doc.setAnalytics(updatedAnalytics);

                Instant now = Instant.now();
                if (doc.getSectionLastUpdated() == null) {
                    doc.setSectionLastUpdated(new HashMap<>());
                }
                doc.getSectionLastUpdated().put("incomeStatement", now);
                doc.getSectionLastUpdated().put("balanceSheet", now);
                doc.getSectionLastUpdated().put("cashFlow", now);
                doc.getSectionLastUpdated().put("shareholding", now);
                doc.setLastUpdated(now);

                fundamentalDataRepository.save(doc);

                // Rate-limit safe delay
                Thread.sleep(600);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Quarterly fundamental sync interrupted");
                break;
            } catch (Exception e) {
                log.error("Error refreshing quarterly statements for isin={}: {}", isin, e.getMessage());
            }
        }
        log.info("Completed quarterly financial statements and shareholdings sync");
    }
}
