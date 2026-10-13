package com.am.marketdata.service.fundamental;

import com.am.marketdata.common.model.fundamental.BalanceSheetEntry;
import com.am.marketdata.common.model.fundamental.CashFlowEntry;
import com.am.marketdata.common.model.fundamental.FundamentalAnalytics;
import com.am.marketdata.common.model.fundamental.IncomeStatementEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure domain calculation engine for computing derived fundamental metrics.
 * Implements strict sanity safeguards (e.g. division-by-zero checks, null handling).
 */
@Slf4j
@Component
public class FundamentalCalculationEngine {

    /**
     * Computes derived analytical metrics from financial statements and historical price indicators.
     *
     * @param incomeStatements list of IncomeStatementEntry
     * @param balanceSheets list of BalanceSheetEntry
     * @param cashFlows list of CashFlowEntry
     * @param week52High 52-week High price from market data
     * @param week52Low 52-week Low price from market data
     * @param cagr1Y 1-year price CAGR
     * @param cagr3Y 3-year price CAGR
     * @param cagr5Y 5-year price CAGR
     * @return populated {@link FundamentalAnalytics}
     */
    public FundamentalAnalytics computeAnalytics(List<IncomeStatementEntry> incomeStatements,
                                                 List<BalanceSheetEntry> balanceSheets,
                                                 List<CashFlowEntry> cashFlows,
                                                 Double week52High,
                                                 Double week52Low,
                                                 Double cagr1Y,
                                                 Double cagr3Y,
                                                 Double cagr5Y) {
        FundamentalAnalytics.FundamentalAnalyticsBuilder builder = FundamentalAnalytics.builder()
                .week52High(week52High)
                .week52Low(week52Low)
                .priceCagr1Y(cagr1Y)
                .priceCagr3Y(cagr3Y)
                .priceCagr5Y(cagr5Y);

        // 1. Current Ratio = Current Assets / Current Liabilities (from latest balance sheet)
        if (balanceSheets != null && !balanceSheets.isEmpty()) {
            BalanceSheetEntry latestBs = balanceSheets.get(0);
            if (latestBs.getCurrentAssets() != null && latestBs.getCurrentLiabilities() != null 
                    && latestBs.getCurrentLiabilities() > 0) {
                double currentRatio = latestBs.getCurrentAssets() / latestBs.getCurrentLiabilities();
                builder.currentRatio(round(currentRatio, 2));
            }
        }

        // 2. Margins (Operating Margin & Net Margin) from latest Income Statement
        IncomeStatementEntry latestInc = null;
        if (incomeStatements != null && !incomeStatements.isEmpty()) {
            latestInc = incomeStatements.get(0);
            Double totalRev = latestInc.getTotalRevenue() != null ? latestInc.getTotalRevenue() : latestInc.getRevenue();

            if (totalRev != null && totalRev > 0) {
                if (latestInc.getOperatingProfit() != null) {
                    double opMargin = (latestInc.getOperatingProfit() / totalRev) * 100.0;
                    builder.operatingMarginPercent(round(opMargin, 2));
                }
                if (latestInc.getProfitAfterTax() != null) {
                    double netMargin = (latestInc.getProfitAfterTax() / totalRev) * 100.0;
                    builder.netProfitMarginPercent(round(netMargin, 2));
                }
            }
        }

        // 3. CFO / PAT = Operating Cash Flow / Profit After Tax
        if (cashFlows != null && !cashFlows.isEmpty() && latestInc != null) {
            CashFlowEntry latestCf = cashFlows.get(0);
            Double ocf = latestCf.getOperatingCashFlow();
            Double pat = latestInc.getProfitAfterTax();

            if (ocf != null && pat != null && pat != 0) {
                double cfoPat = ocf / pat;
                builder.cfoPat(round(cfoPat, 2));
            }
        }

        return builder.build();
    }

    /**
     * Calculates Compound Annual Growth Rate (CAGR).
     * Formula: (EndingValue / StartingValue) ^ (1 / years) - 1
     *
     * @param startingPrice initial price at start of period
     * @param endingPrice final price at end of period
     * @param years number of years
     * @return CAGR percentage (rounded to 2 decimal places) or null if invalid
     */
    public Double calculateCagr(Double startingPrice, Double endingPrice, double years) {
        if (startingPrice == null || endingPrice == null || startingPrice <= 0 || endingPrice <= 0 || years <= 0) {
            return null;
        }
        try {
            double cagr = (Math.pow(endingPrice / startingPrice, 1.0 / years) - 1.0) * 100.0;
            return round(cagr, 2);
        } catch (Exception e) {
            log.warn("Error calculating CAGR start={} end={} years={}: {}", startingPrice, endingPrice, years, e.getMessage());
            return null;
        }
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
