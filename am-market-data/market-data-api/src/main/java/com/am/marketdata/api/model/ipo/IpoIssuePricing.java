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
@Schema(name = "IpoIssuePricing", description = "Issue price band / fixed price")
public class IpoIssuePricing {

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Price band minimum", example = "214.0")
    private Double priceMin;

    @Schema(description = "Price band maximum", example = "225.0")
    private Double priceMax;

    @Schema(description = "Final / fixed issue price when available", example = "225.0")
    private Double issuePrice;

    @Schema(description = "Original price label from vendor", example = "Rs.214 to Rs.225")
    private String label;
}
