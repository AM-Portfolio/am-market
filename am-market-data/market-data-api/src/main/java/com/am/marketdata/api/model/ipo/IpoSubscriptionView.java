package com.am.marketdata.api.model.ipo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IpoSubscription", description = "Category-wise IPO subscription / bid details")
public class IpoSubscriptionView {

    @Schema(description = "Parent issue id", example = "JNPR:2026-07-30")
    private String issueId;

    @Schema(description = "Symbol", example = "JNPR")
    private String symbol;

    @Schema(description = "Series used for bid lookup", example = "EQ")
    private String series;

    @Schema(description = "Vendor update timestamp", example = "2026-08-03T19:01:11+05:30")
    private OffsetDateTime updatedAt;

    private IpoSubscriptionOverall overall;

    @ArraySchema(schema = @Schema(implementation = IpoSubscriptionCategoryView.class))
    @Builder.Default
    private List<IpoSubscriptionCategoryView> categories = new ArrayList<>();
}
