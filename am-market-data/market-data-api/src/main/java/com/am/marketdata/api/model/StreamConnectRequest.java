package com.am.marketdata.api.model;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class StreamConnectRequest {
    private List<String> instrumentKeys;
    private String mode; // e.g., "FULL", "LTPC"

    @Builder.Default
    private Boolean expandIndices = false; // Whether to expand index symbols to constituents (default: false)

    @Builder.Default
    private String timeFrame = "1D"; // TimeFrame for historical data calculation (default: 1D)

    @Builder.Default
    private Boolean isIndexSymbol = false; // Whether the symbol is an index (for caching purposes)

    @Builder.Default
    private Boolean stream = true; // Whether to start a continuous stream (default: true)

    private String provider; // Optional provider

    @Builder.Default
    private Boolean forcePolling = false; // Whether to force polling mode (simulated stream) even if market is
                                          // closed/open
}
