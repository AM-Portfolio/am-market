package com.am.marketdata.api.model.ipo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IpoSyncResponse", description = "Result of an admin IPO sync call")
public class IpoSyncResponse {

    @Schema(description = "Requested feed scope", example = "CURRENT")
    private String scope;

    @Schema(description = "Outcome", example = "ok", allowableValues = {"ok", "failed"})
    private String status;

    @Schema(description = "Upserts / successful subscription refreshes", example = "3")
    private Integer upserts;

    @Schema(description = "Error message when status=failed", example = "No valid cookies found in cache")
    private String error;

    private IpoSyncMetaView meta;
}
