package com.am.marketdata.scraper.mapper;

import com.am.common.investment.model.equity.ETFIndies;
import com.am.common.investment.model.equity.MarketData;
import com.am.common.investment.model.equity.MetaData;
import com.am.marketdata.common.model.NseETF;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("etfIndicesMapper")
public class ETFIndicesMapper {

    public static List<ETFIndies> convertToETFIndices(List<NseETF> data) {
        return data.stream()
            .map(ETFIndicesMapper::convertToMarketIndices)
            .collect(Collectors.toList());
    }
    
    public static ETFIndies convertToMarketIndices(NseETF data) {
        return ETFIndies.builder()
            .assets(data.getAssets())
            .symbol(data.getSymbol())
            .timestamp(LocalDateTime.now()) // Using current time instead of fixed timestamp
            .marketData(MarketData.builder()
                .open(data.getOpen())
                .high(data.getHigh())
                .low(data.getLow())
                .last(data.getLastTradedPrice())
                .previousClose(data.getPrevClose())
                .percentChange(data.getPercentChange())
                .build())
            .metaData(MetaData.builder()
                .symbol(data.getSymbol())
                .companyName(data.getMeta().getCompanyName())
                .isin(data.getMeta().getIsin())
                .build())
            .build();
    }
}
