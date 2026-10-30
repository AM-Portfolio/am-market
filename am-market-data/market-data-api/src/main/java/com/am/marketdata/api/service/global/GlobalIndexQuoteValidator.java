package com.am.marketdata.api.service.global;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.service.global.GlobalMarketScheduleService;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** Validates provider event-time quotes used only by the global-index API path. */
final class GlobalIndexQuoteValidator {

    private static final long FUTURE_CLOCK_SKEW_MS = TimeUnit.MINUTES.toMillis(2);
    private static final long PROVIDER_DELAY_GRACE_SECONDS = 120L;

    private GlobalIndexQuoteValidator() {}

    static boolean isUsable(OHLCQuote quote, GlobalMarketScheduleService.MarketEntry config,
                            GlobalMarketScheduleService scheduleService, Instant now) {
        if (quote == null || !Double.isFinite(quote.getLastPrice()) || quote.getLastPrice() <= 0
                || !Double.isFinite(quote.getPreviousClose()) || quote.getPreviousClose() <= 0
                || quote.getQuoteTimestamp() == null || config == null || !config.hasInstrument()) return false;
        Instant eventTime = quote.getQuoteTimestamp();
        if (eventTime.isAfter(now.plusMillis(FUTURE_CLOCK_SKEW_MS))) return false;
        if (scheduleService.isMarketCurrentlyOpen(config, now)) {
            long ageMs = Math.max(0, now.toEpochMilli() - eventTime.toEpochMilli());
            long configuredDelayMs = TimeUnit.SECONDS.toMillis(config.quoteMaxAgeSeconds() + PROVIDER_DELAY_GRACE_SECONDS);
            return ageMs <= configuredDelayMs;
        }
        return scheduleService.quoteBelongsToLatestCompletedSession(config, eventTime, now);
    }

    static boolean isProviderDelayed(OHLCQuote quote, GlobalMarketScheduleService.MarketEntry config,
                                     GlobalMarketScheduleService scheduleService, Instant now) {
        return quote == null || quote.getQuoteTimestamp() == null
                || config == null || !config.hasInstrument()
                || scheduleService.isMarketCurrentlyOpen(config, now)
                && now.toEpochMilli() - quote.getQuoteTimestamp().toEpochMilli()
                > TimeUnit.SECONDS.toMillis(config.quoteMaxAgeSeconds() + PROVIDER_DELAY_GRACE_SECONDS);
    }
}
