package com.am.marketdata.api.model.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MarketStatus", description = "Live market open/closed status for an exchange")
public class MarketStatusResponse {

    @Schema(description = "Exchange code", example = "NSE")
    private String exchange;

    @Schema(description = "True when the market is currently open for trading", example = "true")
    private boolean open;

    @Schema(
            description = "Status reason code",
            example = "OPEN",
            allowableValues = {
                    "OPEN",
                    "WEEKEND",
                    "HOLIDAY",
                    "HOLIDAY_FALLBACK",
                    "OUTSIDE_SESSION"
            })
    private String reason;

    @Schema(description = "Server evaluation timestamp (UTC)", example = "2026-08-04T05:30:00Z")
    private Instant asOf;

    @Schema(description = "Today session start in exchange local time", example = "09:15:00")
    private LocalTime sessionStart;

    @Schema(description = "Today session end in exchange local time", example = "15:30:00")
    private LocalTime sessionEnd;

    @Schema(description = "Response metadata")
    private MarketCalendarMetaView meta;
}
