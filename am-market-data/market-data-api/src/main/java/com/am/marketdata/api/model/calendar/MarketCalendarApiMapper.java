package com.am.marketdata.api.model.calendar;

import com.am.marketdata.service.calendar.MarketCalendarService;
import com.am.marketdata.service.model.MarketCalendarDayDocument;

import java.util.List;
import java.util.stream.Collectors;

public final class MarketCalendarApiMapper {

    private MarketCalendarApiMapper() {}

    public static MarketCalendarHolidaysResponse toHolidays(MarketCalendarService.CalendarQueryResult result) {
        List<MarketCalendarDayView> days = result.days().stream()
                .map(MarketCalendarApiMapper::toDay)
                .collect(Collectors.toList());
        return MarketCalendarHolidaysResponse.builder()
                .data(days)
                .meta(toMeta(result.meta()))
                .build();
    }

    public static MarketSessionTimingResponse toTiming(MarketCalendarService.SessionTiming timing) {
        return MarketSessionTimingResponse.builder()
                .exchange(timing.exchange())
                .date(timing.date())
                .open(timing.open())
                .sessionStart(timing.sessionStart())
                .sessionEnd(timing.sessionEnd())
                .reason(timing.reason())
                .meta(toMeta(timing.meta()))
                .build();
    }

    public static MarketStatusResponse toStatus(MarketCalendarService.MarketStatusView status) {
        return MarketStatusResponse.builder()
                .exchange(status.exchange())
                .open(status.open())
                .reason(status.reason())
                .asOf(status.asOf())
                .sessionStart(status.sessionStart())
                .sessionEnd(status.sessionEnd())
                .meta(toMeta(status.meta()))
                .build();
    }

    public static MarketCalendarDayView toDay(MarketCalendarDayDocument day) {
        return MarketCalendarDayView.builder()
                .date(day.getDate())
                .exchange(day.getExchange())
                .description(day.getDescription())
                .holidayType(day.getHolidayType())
                .closed(day.isClosed())
                .sessionStart(day.getSessionStart())
                .sessionEnd(day.getSessionEnd())
                .source(day.getSource())
                .build();
    }

    public static MarketCalendarMetaView toMeta(MarketCalendarService.CalendarMeta meta) {
        return MarketCalendarMetaView.builder()
                .stale(meta.stale())
                .source(meta.source())
                .lastFullSyncAt(meta.lastFullSyncAt())
                .build();
    }
}
