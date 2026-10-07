package com.am.marketdata.external.upstox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Raw Upstox JSON DTO mapping eligible investor categories.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxInvestorCategoryDto {

    @JsonProperty("category")
    private String category;

    @JsonProperty("description")
    private String description;
}
