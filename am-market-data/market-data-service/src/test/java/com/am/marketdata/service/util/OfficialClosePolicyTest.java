package com.am.marketdata.service.util;

import com.am.common.investment.model.historical.OHLCVTPoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfficialClosePolicyTest {

    private static OHLCVTPoint bar(LocalDate date, double close) {
        OHLCVTPoint p = new OHLCVTPoint();
        p.setTime(date.atTime(15, 30));
        p.setClose(close);
        return p;
    }

    @Test
    void beforeOpenOnSessionDay_usesLastSessionClose() {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        List<OHLCVTPoint> points = List.of(
                bar(LocalDate.of(2026, 9, 17), 1243.9),
                bar(LocalDate.of(2026, 9, 18), 1226.4));
        Double close = OfficialClosePolicy.pickSessionClose(points, monday, true, false);
        assertEquals(1226.4, close);
    }

    @Test
    void afterCloseOnSessionDay_requiresTodayCandle() {
        LocalDate thursday = LocalDate.of(2026, 9, 18);
        List<OHLCVTPoint> points = List.of(
                bar(LocalDate.of(2026, 9, 17), 1243.9),
                bar(LocalDate.of(2026, 9, 18), 1226.4));
        assertEquals(1226.4, OfficialClosePolicy.pickSessionClose(points, thursday, true, true));
        // Friday after close with only Thu bar → null (do not paint Thu as Fri)
        LocalDate friday = LocalDate.of(2026, 9, 19);
        assertNull(OfficialClosePolicy.pickSessionClose(points, friday, true, true));
    }

    @Test
    void weekend_usesLatestSession() {
        LocalDate sunday = LocalDate.of(2026, 9, 20);
        List<OHLCVTPoint> points = List.of(bar(LocalDate.of(2026, 9, 18), 1226.4));
        assertEquals(1226.4, OfficialClosePolicy.pickSessionClose(points, sunday, false, false));
    }
}
