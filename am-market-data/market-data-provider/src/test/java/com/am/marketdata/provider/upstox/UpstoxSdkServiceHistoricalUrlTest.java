package com.am.marketdata.provider.upstox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpstoxSdkServiceHistoricalUrlTest {

    @Test
    void globalDailyHistoryUsesV3AndPreservesRangeAndEncodedKey() {
        String url = UpstoxSdkService.buildHistoricalCandleUrl(
                "GLOBAL_INDEX%7C%5EDJI", "GLOBAL_INDEX|^DJI", "days", 1,
                "2026-10-23", "2016-10-23", false);

        assertEquals("https://api.upstox.com/v3/historical-candle/GLOBAL_INDEX%7C%5EDJI/days/1/2026-10-23/2016-10-23", url);
    }

    @Test
    void globalMonthlyHistoryUsesV3AndOmitsOptionalFromDateWhenNotNeeded() {
        String url = UpstoxSdkService.buildHistoricalCandleUrl(
                "GLOBAL_INDEX%7CSGX%20NIFTY", "GLOBAL_INDEX|SGX NIFTY", "months", 1,
                "2026-10-23", "2026-10-23", false);

        assertEquals("https://api.upstox.com/v3/historical-candle/GLOBAL_INDEX%7CSGX%20NIFTY/months/1/2026-10-23", url);
    }

    @Test
    void nonGlobalDailyHistoryRetainsExistingV2Contract() {
        String url = UpstoxSdkService.buildHistoricalCandleUrl(
                "NSE_EQ%7CINE848E01016", "NSE_EQ|INE848E01016", "days", 1,
                "2026-10-23", "2026-10-01", false);

        assertEquals("https://api.upstox.com/v2/historical-candle/NSE_EQ%7CINE848E01016/day/2026-10-23/2026-10-01", url);
    }
}
