package com.am.marketdata.external.upstox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Raw Upstox JSON DTO mapping full detailed element returned by GET /v2/ipos/{id}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxIpoDetailsResponseDto {

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

    @JsonProperty("daily_start_time")
    private String dailyStartTime;

    @JsonProperty("daily_end_time")
    private String dailyEndTime;

    @JsonProperty("face_value")
    private Double faceValue;

    @JsonProperty("tick_size")
    private Double tickSize;

    @JsonProperty("lot_size")
    private Integer lotSize;

    @JsonProperty("minimum_quantity")
    private Integer minimumQuantity;

    @JsonProperty("cut_off_price")
    private Double cutOffPrice;

    @JsonProperty("listing_price")
    private Double listingPrice;

    @JsonProperty("listing_exchange")
    private String listingExchange;

    @JsonProperty("rhp_url")
    private String rhpUrl;

    @JsonProperty("drhp_url")
    private String drhpUrl;

    @JsonProperty("timeline")
    private UpstoxTimelineDto timeline;

    @JsonProperty("registrar_info")
    private UpstoxRegistrarInfoDto registrarInfo;

    @JsonProperty("total_subscription")
    private Object totalSubscription;

    public String getTotalSubscription() {
        return totalSubscription != null ? String.valueOf(totalSubscription) : null;
    }

    @JsonProperty("investors")
    private List<UpstoxInvestorCategoryDto> investors;
}
