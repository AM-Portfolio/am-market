package com.am.marketdata.api.model;

import com.am.marketdata.common.model.fundamental.KeyRatios;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Named DTO response for valuation and profitability key ratios (AM SDK Gate compliant).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Valuation and profitability ratios for a company")
public class FundamentalRatiosResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Key valuation ratios (P/E, P/B, EV/EBITDA, Dividend Yield)")
    private KeyRatios valuation;

    @Schema(description = "Profitability metrics (ROA, ROE, ROCE)")
    private FundamentalAnalysisResponse.ProfitabilitySection profitability;
}
