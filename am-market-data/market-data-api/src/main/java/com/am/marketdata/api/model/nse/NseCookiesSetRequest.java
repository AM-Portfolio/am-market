package com.am.marketdata.api.model.nse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NseCookiesSetRequest", description = "Paste browser Cookie header for NSE India session")
public class NseCookiesSetRequest {

    @Schema(
            description = "Full Cookie header value from an authenticated browser session on nseindia.com",
            example = "nsit=abc; nseappid=eyJ...; bm_sv=...")
    private String cookieHeader;

    @Schema(description = "Optional TTL override in minutes (default from config, usually 60)", example = "60")
    private Integer ttlMinutes;
}
