package com.am.marketdata.api.model.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MarketCalendarDay", description = "Holiday or special-session day for an exchange")
public class MarketCalendarDayView {

    @Schema(description = "Calendar date (ISO-8601)", example = "2026-01-26")
    private LocalDate date;

    @Schema(description = "Exchange code", example = "NSE", allowableValues = {"NSE", "BSE", "NFO", "BFO", "CDS", "BCD", "MCX"})
    private String exchange;

    @Schema(description = "Human-readable holiday or event description", example = "Republic Day")
    private String description;

    @Schema(description = "Holiday classification from vendor", example = "TRADING_HOLIDAY")
    private String holidayType;

    @Schema(description = "True when the market is fully closed for this date", example = "true")
    private boolean closed;

    @Schema(description = "Session start time when the day has special timings; null for full closures", example = "09:15:00")
    private LocalTime sessionStart;

    @Schema(description = "Session end time when the day has special timings; null for full closures", example = "15:30:00")
    private LocalTime sessionEnd;

    @Schema(description = "Vendor source that produced this day", example = "UPSTOX")
    private String source;
}
