package com.am.marketdata.api.model;

import com.am.marketdata.common.model.fundamental.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Unified normalized fundamental analysis response returned by the REST API (Phase 11 Compliance).
 * Decouples the frontend from third-party broker schemas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundamentalAnalysisResponse {

    /**
     * High-level company information and real-time market data.
     */
    private CompanyOverviewSection company;

    /**
     * Key valuation ratios compared against sector benchmarks.
     */
    private KeyRatios valuation;

    /**
     * Profitability ratios (ROE, ROCE, ROA).
     */
    private ProfitabilitySection profitability;

    /**
     * Financial statement histories (Income Statement, Balance Sheet, Cash Flow).
     */
    private FinancialsSection financials;

    /**
     * Historical quarterly shareholding breakdown.
     */
    private List<ShareholdingQuarterEntry> shareholding;

    /**
     * Corporate actions history (Dividends, Bonus, Splits, Rights).
     */
    private List<CorporateActionEntry> corporateActions;

    /**
     * Peer companies and competitor comparison table.
     */
    private List<CompetitorPeer> peers;

    /**
     * Calculated health analytics (Margins, Current Ratio, CFO/PAT, 52W High/Low, CAGR).
     */
    private FundamentalAnalytics analytics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyOverviewSection implements Serializable {
        private String symbol;
        private String isin;
        private String companyName;
        private String description;
        private String sector;
        private Double sectorMarketCapInr;
        private Double sectorMarketCapUsd;
        private Double currentPrice;
        private Double dayHigh;
        private Double dayLow;
        private Double dayChange;
        private Double dayChangePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitabilitySection implements Serializable {
        private Double roa;
        private Double sectorRoa;
        private Double roe;
        private Double sectorRoe;
        private Double roce;
        private Double sectorRoce;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialsSection implements Serializable {
        private List<IncomeStatementEntry> incomeStatement;
        private List<BalanceSheetEntry> balanceSheet;
        private List<CashFlowEntry> cashFlow;
    }
}
