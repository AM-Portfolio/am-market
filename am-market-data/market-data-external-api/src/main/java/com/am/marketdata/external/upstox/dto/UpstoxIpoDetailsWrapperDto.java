package com.am.marketdata.external.upstox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Top-level envelope DTO for GET /v2/ipos/{id} response.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxIpoDetailsWrapperDto {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private UpstoxIpoDetailsResponseDto data;
}
