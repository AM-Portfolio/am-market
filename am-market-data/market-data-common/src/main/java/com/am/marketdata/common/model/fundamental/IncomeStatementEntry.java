package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Single reporting period line-item breakdown for the Income Statement (P&L).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Reporting period label (e.g., "Mar 2026", "Dec 2025").
     */
    private String period;

    /**
     * Financial reporting type: "consolidated" or "standalone".
     */
    private String type;

    /**
     * Time cadence: "yearly" or "quarterly".
     */
    private String timePeriod;

    /**
     * Currency unit (e.g., "₹ crore").
     */
    private String unit;

    // Line items (in ₹ crore)
    private Double revenue;
    private Double otherIncome;
    private Double totalRevenue;
    private Double totalExpenses;
    private Double operatingProfit;
    private Double profitBeforeTax;
    private Double tax;
    private Double profitAfterTax;
    private Double epsBasic;
    private Double epsDiluted;

    // Period-over-period percentage growth metrics (as provided directly by source)
    private Double revenueChangePercent;
    private Double operatingProfitChangePercent;
    private Double netProfitChangePercent;
}
