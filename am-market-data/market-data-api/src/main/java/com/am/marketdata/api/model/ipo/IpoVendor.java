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
@Schema(name = "IpoVendor", description = "Upstream vendor attribution")
public class IpoVendor {

    @Schema(description = "Vendor source id", example = "NSE")
    private String source;
}
