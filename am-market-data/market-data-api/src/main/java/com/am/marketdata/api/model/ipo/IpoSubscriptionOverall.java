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
@Schema(name = "IpoSubscriptionOverall", description = "Overall subscription totals")
public class IpoSubscriptionOverall {

    @Schema(example = "6.12")
    private Double times;

    @Schema(example = "360427056")
    private Long sharesBid;

    @Schema(example = "58916709")
    private Long sharesOffered;
}
