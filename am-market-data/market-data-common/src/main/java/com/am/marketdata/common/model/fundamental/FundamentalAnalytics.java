package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Derived analytical metrics computed from raw financial statements and historical price candles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundamentalAnalytics implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Current Ratio = Current Assets / Current Liabilities (Liquidity check).
     */
    private Double currentRatio;

    /**
     * CFO / PAT = Operating Cash Flow / Profit After Tax (Earnings quality ratio).
     */
    private Double cfoPat;

    /**
     * Operating Profit Margin (%) = (Operating Profit / Total Revenue) * 100.
     */
    private Double operatingMarginPercent;

    /**
     * Net Profit Margin (%) = (Net Profit / Total Revenue) * 100.
     */
    private Double netProfitMarginPercent;

    /**
     * 52-Week High price (₹) calculated over the past 365 daily candles.
     */
    private Double week52High;

    /**
     * 52-Week Low price (₹) calculated over the past 365 daily candles.
     */
    private Double week52Low;

    /**
     * 1-Year Price Compound Annual Growth Rate (CAGR) (%).
     */
    private Double priceCagr1Y;

    /**
     * 3-Year Price Compound Annual Growth Rate (CAGR) (%).
     */
    private Double priceCagr3Y;

    /**
     * 5-Year Price Compound Annual Growth Rate (CAGR) (%).
     */
    private Double priceCagr5Y;
}
