package com.am.marketdata.api.controller;

import com.am.marketdata.api.model.calendar.MarketCalendarHolidaysResponse;
import com.am.marketdata.api.model.calendar.MarketSessionTimingResponse;
import com.am.marketdata.api.model.calendar.MarketStatusResponse;
import com.am.marketdata.api.service.MarketCalendarApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/market-calendar")
@RequiredArgsConstructor
@Tag(
        name = "Market Calendar",
        description = "Exchange holiday calendar, session timings, and live market open status. "
                + "Data is served from local Mongo (synced from Upstox). Supported exchanges: "
                + "NSE, BSE, NFO, BFO, CDS, BCD, MCX.")
public class MarketCalendarController {

    private final MarketCalendarApiService marketCalendarApiService;

    @GetMapping(value = "/holidays", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "List holidays for a year",
            description = "Returns holiday and special-session days for the exchange and calendar year. "
                    + "If local data is missing, a one-time lazy sync may run before responding.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Holiday list",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MarketCalendarHolidaysResponse.class)))
    })
    public ResponseEntity<MarketCalendarHolidaysResponse> holidays(
            @Parameter(description = "Exchange code", example = "NSE")
            @RequestParam(defaultValue = "NSE") String exchange,
            @Parameter(description = "Calendar year; defaults to current year", example = "2026")
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(marketCalendarApiService.getHolidays(exchange, year));
    }

    @GetMapping(value = "/holidays/{date}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get holiday for a date",
            description = "Returns the calendar entry for a single date. Empty data array means a normal trading day "
                    + "(no holiday override).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Holiday lookup result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MarketCalendarHolidaysResponse.class)))
    })
    public ResponseEntity<MarketCalendarHolidaysResponse> holiday(
            @Parameter(description = "Date (ISO-8601)", example = "2026-01-26", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Exchange code", example = "NSE")
            @RequestParam(defaultValue = "NSE") String exchange) {
        return ResponseEntity.ok(marketCalendarApiService.getHoliday(exchange, date));
    }

    @GetMapping(value = "/timings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get session timings for a date",
            description = "Resolves whether the exchange has a session on the given date and the start/end times. "
                    + "Reason codes: WEEKEND, HOLIDAY, CALENDAR (special session), CONFIG_DEFAULT.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Session timing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MarketSessionTimingResponse.class)))
    })
    public ResponseEntity<MarketSessionTimingResponse> timings(
            @Parameter(description = "Date (ISO-8601)", example = "2026-08-04", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Exchange code", example = "NSE")
            @RequestParam(defaultValue = "NSE") String exchange) {
        return ResponseEntity.ok(marketCalendarApiService.getTimings(exchange, date));
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get live market status",
            description = "Returns whether the exchange is open right now in Asia/Kolkata market time, "
                    + "including today's session window and a reason code.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Live market status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MarketStatusResponse.class)))
    })
    public ResponseEntity<MarketStatusResponse> status(
            @Parameter(description = "Exchange code", example = "NSE")
            @RequestParam(defaultValue = "NSE") String exchange) {
        return ResponseEntity.ok(marketCalendarApiService.getStatus(exchange));
    }
}
