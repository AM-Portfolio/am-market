package com.am.marketdata.external.upstox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Raw Upstox JSON DTO mapping key IPO timeline dates.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxTimelineDto {

    @JsonProperty("pre_apply_start_date")
    private String preApplyStartDate;

    @JsonProperty("application_start_date")
    private String applicationStartDate;

    @JsonProperty("application_end_date")
    private String applicationEndDate;

    @JsonProperty("allotment_start_date")
    private String allotmentStartDate;

    @JsonProperty("allotment_date")
    private String allotmentDate;

    @JsonProperty("refund_initiation_date")
    private String refundInitiationDate;

    @JsonProperty("listing_date")
    private String listingDate;

    @JsonProperty("mandate_end_date")
    private String mandateEndDate;
}
