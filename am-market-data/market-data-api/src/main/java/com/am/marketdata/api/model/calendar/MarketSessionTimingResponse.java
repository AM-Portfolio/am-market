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
@Schema(name = "MarketSessionTiming", description = "Resolved trading session for a specific exchange date")
public class MarketSessionTimingResponse {

    @Schema(description = "Exchange code", example = "NSE")
    private String exchange;

    @Schema(description = "Trading date (ISO-8601)", example = "2026-08-04")
    private LocalDate date;

    @Schema(description = "True when a trading session is expected on this date", example = "true")
    private boolean open;

    @Schema(description = "Session start in exchange local time; null when closed", example = "09:15:00")
    private LocalTime sessionStart;

    @Schema(description = "Session end in exchange local time; null when closed", example = "15:30:00")
    private LocalTime sessionEnd;

    @Schema(
            description = "Why this timing was chosen",
            example = "CONFIG_DEFAULT",
            allowableValues = {"WEEKEND", "HOLIDAY", "CALENDAR", "CONFIG_DEFAULT"})
    private String reason;

    @Schema(description = "Response metadata")
    private MarketCalendarMetaView meta;
}
