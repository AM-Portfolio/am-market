package com.am.common.investment.model.stockindice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data model for individual stock information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockData {
    private String symbol;
    private String identifier;
    private String series;
    private String name;
    private Long ffmc;
    private String companyName;
    private String isin;
    private String industry;
}
