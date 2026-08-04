package com.am.marketdata.common.calendar;

import java.util.List;

/**
 * Port for remote market calendar fetch. Implementations are vendor-specific (Upstox, NSE, etc.).
 * Sync/API layers must depend only on this interface and {@link MarketHolidayDay}.
 */
public interface MarketCalendarSource {

    String sourceId();

    /**
     * Bulk holiday/special-session calendar for the configured market year(s) returned by the vendor.
     */
    List<MarketHolidayDay> fetchHolidays(String exchange);
}
