package com.am.marketdata.provider.upstox.calendar;

import com.am.marketdata.common.calendar.MarketCalendarSource;
import com.am.marketdata.common.calendar.MarketHolidayDay;
import com.am.marketdata.common.calendar.MarketHolidayType;
import com.am.marketdata.provider.upstox.UpstoxSdkService;
import com.upstox.api.ExchangeTimingData;
import com.upstox.api.GetHolidayResponse;
import com.upstox.api.HolidayData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market-data.calendar.source", havingValue = "upstox", matchIfMissing = true)
public class UpstoxMarketCalendarSource implements MarketCalendarSource {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final String SOURCE_ID = "UPSTOX";

    private final UpstoxSdkService upstoxSdkService;

    @Override
    public String sourceId() {
        return SOURCE_ID;
    }

    @Override
    public List<MarketHolidayDay> fetchHolidays(String exchange) {
        String targetExchange = exchange == null || exchange.isBlank() ? "NSE" : exchange.trim().toUpperCase();
        try {
            GetHolidayResponse response = upstoxSdkService.getHolidays();
            if (response == null || response.getData() == null) {
                return List.of();
            }
            List<MarketHolidayDay> days = new ArrayList<>();
            for (HolidayData holiday : response.getData()) {
                days.addAll(mapHoliday(holiday, targetExchange));
            }
            return List.copyOf(days);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch holidays from Upstox for " + targetExchange, e);
        }
    }

    private List<MarketHolidayDay> mapHoliday(HolidayData holiday, String targetExchange) {
        LocalDate date = toLocalDate(holiday.getDate());
        if (date == null) {
            return List.of();
        }
        MarketHolidayType type = mapType(holiday.getHolidayType());
        String description = holiday.getDescription();

        boolean closedForExchange = holiday.getClosedExchanges() != null
                && holiday.getClosedExchanges().stream()
                        .anyMatch(ex -> targetExchange.equalsIgnoreCase(ex));

        if (closedForExchange) {
            return List.of(new MarketHolidayDay(
                    date, targetExchange, description, type, true, null, null));
        }

        if (holiday.getOpenExchanges() == null) {
            return List.of();
        }

        List<MarketHolidayDay> result = new ArrayList<>();
        for (ExchangeTimingData timing : holiday.getOpenExchanges()) {
            if (timing == null || timing.getExchange() == null) {
                continue;
            }
            if (!targetExchange.equalsIgnoreCase(timing.getExchange())) {
                continue;
            }
            LocalTime start = toLocalTime(timing.getStartTime());
            LocalTime end = toLocalTime(timing.getEndTime());
            result.add(new MarketHolidayDay(
                    date,
                    targetExchange,
                    description,
                    type == MarketHolidayType.TRADING_HOLIDAY ? MarketHolidayType.SPECIAL_TIMING : type,
                    false,
                    start,
                    end));
        }
        return result;
    }

    private static MarketHolidayType mapType(HolidayData.HolidayTypeEnum holidayType) {
        if (holidayType == null) {
            return MarketHolidayType.TRADING_HOLIDAY;
        }
        String name = holidayType.name();
        if ("SPECIAL_TIMING".equalsIgnoreCase(name)) {
            return MarketHolidayType.SPECIAL_TIMING;
        }
        return MarketHolidayType.TRADING_HOLIDAY;
    }

    private static LocalDate toLocalDate(org.threeten.bp.OffsetDateTime date) {
        if (date == null) {
            return null;
        }
        return LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static LocalTime toLocalTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis).atZone(IST).toLocalTime();
    }
}
