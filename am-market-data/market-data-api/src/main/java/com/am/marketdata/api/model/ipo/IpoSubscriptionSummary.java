package com.am.marketdata.api.model.ipo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IpoSubscriptionSummary", description = "Coarse overall subscription snapshot")
public class IpoSubscriptionSummary {

    @Schema(description = "Overall subscription times", example = "6.12")
    private Double times;

    @Schema(description = "Overall shares bid", example = "360427056")
    private Long sharesBid;

    @Schema(description = "Overall shares offered", example = "58916709")
    private Long sharesOffered;

    @Schema(description = "Vendor update time when known", example = "2026-08-03T19:01:11+05:30")
    private OffsetDateTime updatedAt;
}
