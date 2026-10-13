package com.am.common.investment.service;

import com.am.common.investment.model.equity.EquityPrice;

import java.util.List;

/**
 * Dedicated Service Interface for Fast Latest-Price Operations.
 * 
 * WHAT PROBLEM IT SOLVES:
 * Bypasses the 30-day historical time-series scans used by EquityService, executing narrow
 * range queries specifically optimized for OHLC endpoints and market quote lookups.
 */
public interface EquityLatestPriceService {

    /**
     * Gets the latest prices for a list of trading symbols using narrow time-window queries (-5d).
     * 
     * @param tradingSymbols List of trading symbols (e.g. ["RELIANCE", "TCS"])
     * @return List of latest EquityPrice objects
     */
    List<EquityPrice> getLatestPricesByTradingSymbols(List<String> tradingSymbols);

    /**
     * Gets the latest prices for a list of ISIN codes using narrow time-window queries (-5d).
     * 
     * @param isins List of ISIN codes
     * @return List of latest EquityPrice objects
     */
    List<EquityPrice> getLatestPricesByIsin(List<String> isins);
}
