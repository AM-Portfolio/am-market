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
@Schema(name = "IpoIssueSize", description = "Issue size")
public class IpoIssueSize {

    @Schema(description = "Shares offered", example = "58916709")
    private Long sharesOffered;

    @Schema(description = "Raw size label when numeric parse is unavailable", example = "58916709")
    private String issueSizeLabel;
}
