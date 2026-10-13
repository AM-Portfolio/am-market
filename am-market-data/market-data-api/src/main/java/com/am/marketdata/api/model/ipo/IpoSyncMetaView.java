package com.am.marketdata.api.model.ipo;

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
@Schema(name = "IpoSyncMeta", description = "Last sync outcome for one IPO feed")
public class IpoSyncMetaView {

    @Schema(description = "Feed id", example = "CURRENT", allowableValues = {"PAST", "CURRENT", "UPCOMING", "SUBSCRIPTION"})
    private String id;

    @Schema(description = "Last successful sync time", example = "2026-08-04T10:00:00Z")
    private Instant lastFullSyncAt;

    @Schema(description = "Last error message when sync failed", example = "No valid cookies found in cache")
    private String lastError;

    @Schema(description = "Rows upserted / subscriptions refreshed on last success", example = "12")
    private Integer lastCount;

    @Schema(description = "What triggered the last sync", example = "ADMIN", allowableValues = {"ADMIN", "SCHEDULER", "STARTUP"})
    private String lastTrigger;

    @Schema(description = "Vendor source", example = "NSE")
    private String source;
}
