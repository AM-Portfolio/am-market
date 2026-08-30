package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Single reporting period line-item breakdown for the Balance Sheet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheetEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Reporting period label (e.g., "Mar 2026", "Mar 2025").
     */
    private String period;

    /**
     * Financial reporting type: "consolidated" or "standalone".
     */
    private String type;

    /**
     * Currency unit (e.g., "₹ crore").
     */
    private String unit;

    // Assets breakdown
    private Double nonCurrentAssets;
    private Double currentAssets;
    private Double totalAssets;

    // Liabilities & Equity breakdown
    private Double currentLiabilities;
    private Double netCurrentAssets;
    private Double nonCurrentLiabilities;
    private Double totalLiabilities;
    private Double equityCapital;
    private Double totalEquityAndLiabilities;
}
