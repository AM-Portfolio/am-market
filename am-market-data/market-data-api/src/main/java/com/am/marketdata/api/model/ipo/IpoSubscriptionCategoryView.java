package com.am.marketdata.api.model.ipo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IpoSubscriptionCategory", description = "Subscription category node; children mirror NSE srNo hierarchy")
public class IpoSubscriptionCategoryView {

    @Schema(description = "Canonical category code", example = "QIB")
    private String code;

    @Schema(description = "Display name", example = "Qualified Institutional Buyers (QIBs)")
    private String name;

    @Schema(description = "NSE serial number used to build the tree", example = "1")
    private String srNo;

    @Schema(example = "1000")
    private Long sharesOffered;

    @Schema(example = "5000")
    private Long sharesBid;

    @Schema(example = "5.0")
    private Double times;

    @ArraySchema(schema = @Schema(implementation = IpoSubscriptionCategoryView.class))
    @Builder.Default
    private List<IpoSubscriptionCategoryView> children = new ArrayList<>();
}
