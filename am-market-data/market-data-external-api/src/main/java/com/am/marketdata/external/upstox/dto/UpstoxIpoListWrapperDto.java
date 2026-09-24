package com.am.marketdata.external.upstox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Top-level envelope DTO for GET /v2/ipos response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxIpoListWrapperDto {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private List<UpstoxIpoItemResponseDto> data;
}
