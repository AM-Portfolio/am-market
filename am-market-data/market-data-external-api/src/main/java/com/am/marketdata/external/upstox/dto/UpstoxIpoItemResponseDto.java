package com.am.marketdata.external.upstox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Raw Upstox JSON DTO mapping single item elements returned by GET /v2/ipos.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxIpoItemResponseDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("isin")
    private String isin;

    @JsonProperty("issue_type")
    private String issueType;

    @JsonProperty("issue_size")
    private Double issueSize;

    @JsonProperty("industry")
    private String industry;

    @JsonProperty("minimum_price")
    private Double minimumPrice;

    @JsonProperty("maximum_price")
    private Double maximumPrice;

    @JsonProperty("bidding_start_date")
    private String biddingStartDate;

    @JsonProperty("bidding_end_date")
    private String biddingEndDate;

    @JsonProperty("total_subscription")
    private String totalSubscription;

    @JsonProperty("investors")
    private List<UpstoxInvestorCategoryDto> investors;
}
