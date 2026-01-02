package com.am.marketdata.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import com.am.marketdata.common.model.TimeFrame;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for historical data API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalDataRequest {

    private String symbols;

    @Schema(description = "Start date in yyyy-MM-dd format", example = "2024-01-01")
    private String from;

    @Schema(description = "End date in yyyy-MM-dd format (optional, defaults to current date)", example = "2024-12-31")
    private String to;

    @Builder.Default
    @JsonDeserialize(using = TimeFrameDeserializer.class)
    private TimeFrame interval = TimeFrame.MINUTE;

    private boolean continuous;

    @Schema(description = "Whether the symbols represent indices that should be expanded to constituent stocks", example = "false")
    @JsonProperty("isIndexSymbol")
    @Builder.Default
    private boolean indexSymbol = false;

    private String instrumentType;

    private boolean forceRefresh;

    @Builder.Default
    private String filterType = "ALL";

    @Builder.Default
    private int filterFrequency = 1;

    private Map<String, Object> additionalParams;
}
