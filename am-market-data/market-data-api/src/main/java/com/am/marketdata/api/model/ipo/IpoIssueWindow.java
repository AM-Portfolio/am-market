package com.am.marketdata.api.model.ipo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IpoIssueWindow", description = "Issue open/close/listing window")
public class IpoIssueWindow {

    @Schema(description = "Issue open date", example = "2026-07-30")
    private LocalDate openDate;

    @Schema(description = "Issue close date", example = "2026-08-01")
    private LocalDate closeDate;

    @Schema(description = "Expected listing date when known", example = "2026-08-08")
    private LocalDate listingDate;

    @Schema(description = "NSE link removal date when known", example = "2026-08-10")
    private LocalDate linkRemovalDate;
}
