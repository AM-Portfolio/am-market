package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Root MongoDB Document representing complete fundamental analysis and historical statements for a stock.
 * Uses `isin` as a unique indexed key to ensure zero duplicate records.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fundamental_analysis")
public class FundamentalData implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    /**
     * Unique International Securities Identification Number (e.g. "INE154A01025").
     * Indexed uniquely to prevent duplicate stock records in MongoDB.
     */
    @Indexed(unique = true)
    private String isin;

    /**
     * Broker trading instrument key (e.g. "NSE_EQ|INE154A01025").
     */
    private String instrumentKey;

    /**
     * Trading symbol (e.g. "ITC").
     */
    private String symbol;

    /**
     * Company full name (e.g. "ITC Ltd").
     */
    private String companyName;

    /**
     * Name of data provider source (e.g. "UPSTOX").
     */
    private String providerSource;

    /**
     * High-level company and sector profile.
     */
    private CompanyProfile companyProfile;

    /**
     * Valuation and profitability key ratios.
     */
    private KeyRatios keyRatios;

    /**
     * Historical Income Statement (P&L) reporting periods (Annual / Yearly).
     */
    private List<IncomeStatementEntry> incomeStatements;

    /**
     * Historical Quarterly Income Statement reporting periods (Q1, Q2, Q3, Q4).
     * Populated via broker-agnostic adapter to enable seamless annual vs quarterly switching in UI.
     */
    private List<IncomeStatementEntry> quarterlyIncomeStatements;

    /**
     * Historical Balance Sheet reporting periods.
     */
    private List<BalanceSheetEntry> balanceSheets;

    /**
     * Historical Cash Flow reporting periods.
     */
    private List<CashFlowEntry> cashFlows;

    /**
     * Historical quarterly shareholding breakdown.
     */
    private List<ShareholdingQuarterEntry> shareholdings;

    /**
     * Historical corporate actions (Dividends, Splits, Bonus, Rights).
     */
    private List<CorporateActionEntry> corporateActions;

    /**
     * Industry competitors and peer comparison metrics.
     */
    private List<CompetitorPeer> peers;

    /**
     * Derived financial health metrics and historical price analytics.
     */
    private FundamentalAnalytics analytics;

    /**
     * Granular section-by-section update timestamps for staleness detection.
     * Keys: "profile", "keyRatios", "incomeStatement", "balanceSheet", "cashFlow", "shareholding", "corporateActions", "competitors".
     */
    private Map<String, Instant> sectionLastUpdated;

    /**
     * Timestamp when the fundamental record was first seeded.
     */
    private Instant createdAt;

    /**
     * Overall last updated timestamp for the document.
     */
    private Instant updatedAt;

    /**
     * Deprecated alias for updatedAt for backward compatibility.
     */
    private Instant lastUpdated;
}
