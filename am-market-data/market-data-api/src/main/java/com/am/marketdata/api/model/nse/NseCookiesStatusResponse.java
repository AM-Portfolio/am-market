package com.am.marketdata.api.model.nse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NseCookiesStatusResponse", description = "Masked NSE cookie cache status (never returns raw cookie values)")
public class NseCookiesStatusResponse {

    @Schema(description = "Whether a cookie string is present in Redis/L1", example = "true")
    private boolean present;

    @Schema(description = "Cookie names only (values never returned)", example = "[\"nsit\",\"nseappid\"]")
    private List<String> cookieNames;

    @Schema(description = "When cookies were last stored")
    private Instant storedAt;

    @Schema(description = "Redis TTL remaining in seconds", example = "3400")
    private Long ttlSecondsRemaining;

    @Schema(description = "Whether Redis is wired as shared store", example = "true")
    private boolean redisBacked;

    @Schema(description = "Outcome for SET calls", example = "ok", allowableValues = {"ok", "failed"})
    private String status;

    @Schema(description = "Error when status=failed")
    private String error;
}
