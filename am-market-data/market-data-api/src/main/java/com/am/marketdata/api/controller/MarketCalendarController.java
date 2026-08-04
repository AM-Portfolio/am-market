package com.am.marketdata.api.controller;

import com.am.marketdata.service.calendar.MarketCalendarService;
import com.am.marketdata.service.model.MarketCalendarDayDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/market-calendar")
@RequiredArgsConstructor
public class MarketCalendarController {

    private final MarketCalendarService marketCalendarService;

    @GetMapping("/holidays")
    public ResponseEntity<Map<String, Object>> holidays(
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : Year.now().getValue();
        MarketCalendarService.CalendarQueryResult result =
                marketCalendarService.getHolidays(exchange, y);
        return ResponseEntity.ok(body(result.days(), result.meta()));
    }

    @GetMapping("/holidays/{date}")
    public ResponseEntity<Map<String, Object>> holiday(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "NSE") String exchange) {
        MarketCalendarService.CalendarQueryResult result =
                marketCalendarService.getHoliday(exchange, date);
        return ResponseEntity.ok(body(result.days(), result.meta()));
    }

    @GetMapping("/timings")
    public ResponseEntity<Map<String, Object>> timings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "NSE") String exchange) {
        MarketCalendarService.SessionTiming timing = marketCalendarService.getTimings(exchange, date);
        Map<String, Object> payload = new HashMap<>();
        payload.put("exchange", timing.exchange());
        payload.put("date", timing.date());
        payload.put("open", timing.open());
        payload.put("sessionStart", timing.sessionStart());
        payload.put("sessionEnd", timing.sessionEnd());
        payload.put("reason", timing.reason());
        payload.put("meta", metaMap(timing.meta()));
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @RequestParam(defaultValue = "NSE") String exchange) {
        MarketCalendarService.MarketStatusView status = marketCalendarService.getStatus(exchange);
        Map<String, Object> payload = new HashMap<>();
        payload.put("exchange", status.exchange());
        payload.put("open", status.open());
        payload.put("reason", status.reason());
        payload.put("asOf", status.asOf());
        payload.put("sessionStart", status.sessionStart());
        payload.put("sessionEnd", status.sessionEnd());
        payload.put("meta", metaMap(status.meta()));
        return ResponseEntity.ok(payload);
    }

    private static Map<String, Object> body(
            List<MarketCalendarDayDocument> days, MarketCalendarService.CalendarMeta meta) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(
                "data",
                days.stream().map(MarketCalendarController::dayMap).collect(Collectors.toList()));
        payload.put("meta", metaMap(meta));
        return payload;
    }

    private static Map<String, Object> dayMap(MarketCalendarDayDocument day) {
        Map<String, Object> m = new HashMap<>();
        m.put("date", day.getDate());
        m.put("exchange", day.getExchange());
        m.put("description", day.getDescription());
        m.put("holidayType", day.getHolidayType());
        m.put("closed", day.isClosed());
        m.put("sessionStart", day.getSessionStart());
        m.put("sessionEnd", day.getSessionEnd());
        m.put("source", day.getSource());
        return m;
    }

    private static Map<String, Object> metaMap(MarketCalendarService.CalendarMeta meta) {
        Map<String, Object> m = new HashMap<>();
        m.put("stale", meta.stale());
        m.put("source", meta.source());
        m.put("lastFullSyncAt", meta.lastFullSyncAt());
        return m;
    }
}
