package com.am.common.investment.model.events.mapper;

import com.am.common.investment.model.events.StockInsidicesEventData;
import com.am.common.investment.model.events.StockInsidicesEventData.IndexMetadata;
import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.model.stockindice.StockData;
import com.am.common.investment.model.stockindice.AuditData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Mapper to convert between StockInsidicesEventData and StockIndicesMarketData
 */
public class StockIndicesEventDataMapper {
    
    public static StockIndicesMarketData toMarketData(StockInsidicesEventData eventData) {
        if (eventData == null) {
            return null;
        }
        
        AuditData auditData = AuditData.builder()
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        return StockIndicesMarketData.builder()
                .indexSymbol(eventData.getName())
                .data(Optional.ofNullable(eventData.getData())
                        .orElse(List.of())
                        .stream()
                        .map(StockIndicesEventDataMapper::mapStockData)
                        .collect(Collectors.toList()))
                .metadata(mapIndexMetadata(eventData.getMetadata()))
                .docVersion("1.0")
                .audit(auditData)
                .build();
    }

    private static IndexMetadata mapIndexMetadata(StockInsidicesEventData.IndexMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        
        return IndexMetadata.builder()
                .indexName(metadata.getIndexName())
                .open(metadata.getOpen())
                .high(metadata.getHigh())
                .low(metadata.getLow())
                .previousClose(metadata.getPreviousClose())
                .last(metadata.getLast())
                .percChange(metadata.getPercChange())
                .change(metadata.getChange())
                .timeVal(metadata.getTimeVal())
                .yearHigh(metadata.getYearHigh())
                .yearLow(metadata.getYearLow())
                .indicativeClose(metadata.getIndicativeClose())
                .totalTradedVolume(metadata.getTotalTradedVolume())
                .totalTradedValue(metadata.getTotalTradedValue())
                .ffmcSum(metadata.getFfmcSum())
                .build();   
    }

    private static StockData mapStockData(StockInsidicesEventData.StockData stockData) {
        if (stockData == null) {
            return null;
        }
        
        StockData.StockDataBuilder stockDataBuilder = StockData.builder()
                .symbol(stockData.getSymbol())
                .identifier(stockData.getIdentifier())
                .series(stockData.getSeries())
                .name(stockData.getSymbol()) // Using symbol as name if needed
                .ffmc(stockData.getFfmc() != null ? stockData.getFfmc().longValue() : null);

        if (stockData.getMetadata() != null) {
            stockDataBuilder
                    .companyName(stockData.getMetadata().getCompanyName())
                    .isin(stockData.getMetadata().getIsin())
                    .industry(stockData.getMetadata().getIndustry());
        }

        return stockDataBuilder.build();
    }
}