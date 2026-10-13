package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Quarterly shareholding pattern breakdown for a company.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareholdingQuarterEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Quarter reporting period label (e.g., "Jun 2026", "Mar 2026").
     */
    private String period;

    /**
     * Percentage of shares held by Promoters (0.0 to 100.0).
     */
    private Double promotersPercent;

    /**
     * Percentage of shares held by Foreign Institutional Investors (FII).
     */
    private Double fiiPercent;

    /**
     * Percentage of shares held by Domestic Institutional Investors (DII) excluding Mutual Funds.
     */
    private Double diiPercent;

    /**
     * Percentage of shares held specifically by Mutual Funds.
     */
    private Double mutualFundsPercent;

    /**
     * Percentage of shares held by Retail and Other Public investors.
     */
    private Double retailAndOtherPercent;
}
