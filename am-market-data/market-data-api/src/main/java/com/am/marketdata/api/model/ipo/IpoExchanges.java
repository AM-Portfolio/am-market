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
@Schema(name = "IpoExchanges", description = "Where the issue is listed")
public class IpoExchanges {

    @Schema(description = "Listed / available on NSE", example = "true")
    private boolean nse;

    @Schema(description = "Also available on BSE", example = "true")
    private boolean bse;
}
