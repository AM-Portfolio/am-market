package com.am.common.investment.model.stockindice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Domain model for stock indices market data without price information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockIndicesMarketData {
    private String indexSymbol;
    private List<StockData> data;
    private IndexMetadata metadata;
    private String docVersion;
    private AuditData audit;
}