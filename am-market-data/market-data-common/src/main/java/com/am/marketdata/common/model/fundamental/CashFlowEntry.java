package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Single reporting period line-item breakdown for the Cash Flow Statement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowEntry implements Serializable {
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

    // Core Cash Flow Categories
    private Double operatingCashFlow;
    private Double investingCashFlow;
    private Double financingCashFlow;
    private Double netCashFlow;

    // Period-over-period percentage growth metrics
    private Double operatingCashFlowChangePercent;
    private Double investingCashFlowChangePercent;
    private Double financingCashFlowChangePercent;
}
