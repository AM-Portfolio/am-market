package com.am.marketdata.api.model.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MarketCalendarMeta", description = "Sync freshness metadata for calendar responses")
public class MarketCalendarMetaView {

    @Schema(description = "True when calendar data is missing, older than the freshness window, or served from fallback rules", example = "false")
    private boolean stale;

    @Schema(description = "Data source identifier", example = "UPSTOX", allowableValues = {"UPSTOX", "FALLBACK", "UNKNOWN"})
    private String source;

    @Schema(description = "Timestamp of the last successful full calendar sync for the exchange", example = "2026-08-04T01:00:00Z")
    private Instant lastFullSyncAt;
}
