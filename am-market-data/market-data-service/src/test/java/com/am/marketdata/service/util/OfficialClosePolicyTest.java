package com.am.marketdata.service.util;

import com.am.common.investment.model.historical.OHLCVTPoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficialClosePolicyTest {

    @Test
    void redisLastTickMustNotWinWhenMarketIsClosed() {
        assertFalse(OfficialClosePolicy.shouldOverlayLiveLastPrice(false));
        assertTrue(OfficialClosePolicy.shouldOverlayLiveLastPrice(true));
    }

    @Test
    void holidayUsesFridayCloseNotStaleLastTrade() {
        LocalDate saturday = LocalDate.of(2026, 8, 15);
        List<OHLCVTPoint> candles = List.of(
                candle(LocalDate.of(2026, 8, 13), 96.33),
                candle(LocalDate.of(2026, 8, 14), 98.09));

        Double close = OfficialClosePolicy.pickSessionClose(candles, saturday, false);

        assertEquals(98.09, close);
    }

    @Test
    void tradingDayAfterCloseWithoutTodayCandleDoesNotUseYesterday() {
        LocalDate friday = LocalDate.of(2026, 8, 14);
        List<OHLCVTPoint> candles = List.of(candle(LocalDate.of(2026, 8, 13), 96.33));

        assertNull(OfficialClosePolicy.pickSessionClose(candles, friday, true));
    }

    @Test
    void tradingDayUsesTodayCandleWhenPresent() {
        LocalDate friday = LocalDate.of(2026, 8, 14);
        List<OHLCVTPoint> candles = List.of(
                candle(LocalDate.of(2026, 8, 13), 125.89),
                candle(LocalDate.of(2026, 8, 14), 125.13));

        assertEquals(125.13, OfficialClosePolicy.pickSessionClose(candles, friday, true));
    }

    private static OHLCVTPoint candle(LocalDate date, double close) {
        return OHLCVTPoint.builder()
                .time(LocalDateTime.of(date, java.time.LocalTime.of(15, 30)))
                .close(close)
                .build();
    }
}
