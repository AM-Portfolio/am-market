package com.am.marketdata.service.util;

import com.am.common.investment.model.historical.OHLCVTPoint;

import java.time.LocalDate;
import java.util.List;

/**
 * After-hours valuation rules. Kept free of Spring so the holiday/weekend
 * close choice can be unit-tested without Redis or Upstox.
 */
public final class OfficialClosePolicy {

    private OfficialClosePolicy() {
    }

    /** Redis last-trade overlay is only valid while the cash market is open. */
    public static boolean shouldOverlayLiveLastPrice(boolean marketOpen) {
        return marketOpen;
    }

    /**
     * @param sessionDay true if calendar today is an NSE cash session date
     *                   (weekday, not holiday)
     * @return last session close, or null if we must not guess
     */
    public static Double pickSessionClose(List<OHLCVTPoint> points, LocalDate today, boolean sessionDay) {
        if (points == null || points.isEmpty() || today == null) {
            return null;
        }
        LocalDate sessionDate = null;
        Double officialClose = null;
        for (OHLCVTPoint point : points) {
            if (point == null || point.getTime() == null || point.getClose() == null || point.getClose() <= 0) {
                continue;
            }
            LocalDate candleDate = point.getTime().toLocalDate();
            if (candleDate.isAfter(today)) {
                continue;
            }
            if (sessionDate == null || candleDate.isAfter(sessionDate)) {
                sessionDate = candleDate;
                officialClose = point.getClose();
            }
        }
        if (officialClose == null || sessionDate == null) {
            return null;
        }
        if (!sessionDate.equals(today) && sessionDay) {
            return null;
        }
        return officialClose;
    }
}
