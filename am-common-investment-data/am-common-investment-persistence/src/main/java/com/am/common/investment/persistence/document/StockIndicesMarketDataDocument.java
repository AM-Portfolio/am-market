package com.am.common.investment.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata;
import com.am.common.investment.model.stockindice.AuditData;
import com.am.common.investment.model.stockindice.StockData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * MongoDB document for stock indices market data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stock_indices_market_data")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockIndicesMarketDataDocument {
    @Id
    private String indexSymbol;
    private List<StockData> data;
    private IndexMetadata metadata;
    private String docVersion;
    private AuditData audit;
}
