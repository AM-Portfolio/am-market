package com.am.common.investment.service.mapper;

import com.am.common.investment.model.equity.FundamentalRatios;
import com.am.common.investment.model.equity.HistoricalComparison;
import com.am.common.investment.model.equity.MarketBreadth;
import com.am.common.investment.model.equity.MarketData;
import com.am.common.investment.model.equity.MarketIndexIndices;
import com.am.common.investment.persistence.influx.measurement.MarketIndexIndicesMeasurement;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MarketIndexIndicesMapper {
    private static final Logger logger = LoggerFactory.getLogger(MarketIndexIndicesMapper.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    
    public MarketIndexIndicesMeasurement convertToMeasurement(MarketIndexIndices indices) {
        if (indices == null) {
            logger.warn("Received null indices object");
            return null;
        }

        MarketIndexIndicesMeasurement measurement = new MarketIndexIndicesMeasurement();
        measurement.setKey(indices.getKey());
        measurement.setIndex(indices.getIndex());
        measurement.setIndexSymbol(indices.getIndexSymbol());
        measurement.setTime(indices.getTimestamp() != null ? indices.getTimestamp().toInstant(ZoneOffset.UTC) : Instant.now());

        // Market Data
        MarketData marketData = indices.getMarketData();
        if (marketData != null) {
            if (marketData.getOpen() != null) measurement.setMarketDataOpen(marketData.getOpen());
            if (marketData.getPreviousClose() != null) measurement.setMarketDataPreviousClose(marketData.getPreviousClose());
            if (marketData.getHigh() != null) measurement.setMarketDataHigh(marketData.getHigh());
            if (marketData.getLow() != null) measurement.setMarketDataLow(marketData.getLow());
            if (marketData.getLast() != null) measurement.setMarketDataClose(marketData.getLast());
            if (marketData.getPercentChange() != null) measurement.setMarketDataPercentageChange(marketData.getPercentChange());
        }

        // Fundamental Ratios
        FundamentalRatios fundamentalRatios = indices.getFundamentalRatios();
        if (fundamentalRatios != null) {
            if (fundamentalRatios.getPriceToEarningRation() != null) measurement.setFundamentalPriceEarning(fundamentalRatios.getPriceToEarningRation());
            if (fundamentalRatios.getPriceToBookRation() != null) measurement.setFundamentalPriceBook(fundamentalRatios.getPriceToBookRation());
            if (fundamentalRatios.getDividenYield() != null) measurement.setFundamentalDividendYield(fundamentalRatios.getDividenYield());
        }

        // Market Breadth
        MarketBreadth marketBreadth = indices.getMarketBreadth();
        if (marketBreadth != null) {
            try {
                if (marketBreadth.getAdvances() != null) {
                    measurement.setMarketBreadthAdvances(Long.valueOf(marketBreadth.getAdvances()));
                }
                if (marketBreadth.getDeclines() != null) {
                    measurement.setMarketBreadthDeclines(Long.valueOf(marketBreadth.getDeclines()));
                }
                if (marketBreadth.getUnchanged() != null) {
                    measurement.setMarketBreadthUnchanged(Long.valueOf(marketBreadth.getUnchanged()));
                }
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse market breadth values, skipping. advances={}, declines={}, unchanged={}", 
                    marketBreadth.getAdvances(), marketBreadth.getDeclines(), marketBreadth.getUnchanged());
            }
        }

        // Historical Comparison
        HistoricalComparison historicalComparison = indices.getHistoricalComparison();
        if (historicalComparison != null) {
            if (historicalComparison.getValue() != null) measurement.setHistoricalComparisonValue(historicalComparison.getValue());
            if (historicalComparison.getPerChange365d() != null) measurement.setHistoricalComparisonPerChange365d(historicalComparison.getPerChange365d());
            if (historicalComparison.getDate365dAgo() != null) measurement.setHistoricalComparisonDate365dAgo(historicalComparison.getDate365dAgo().format(DATE_FORMATTER));
            if (historicalComparison.getPerChange30d() != null) measurement.setHistoricalComparisonPerChange30d(historicalComparison.getPerChange30d());
            if (historicalComparison.getDate30dAgo() != null) measurement.setHistoricalComparisonDate30dAgo(historicalComparison.getDate30dAgo().format(DATE_FORMATTER));
            if (historicalComparison.getPreviousDay() != null) measurement.setHistoricalComparisonPreviousDay(historicalComparison.getPreviousDay());
            if (historicalComparison.getOneWeekAgo() != null) measurement.setHistoricalComparisonOneWeekAgo(historicalComparison.getOneWeekAgo());
            if (historicalComparison.getOneMonthAgo() != null) measurement.setHistoricalComparisonOneMonthAgo(historicalComparison.getOneMonthAgo());
            if (historicalComparison.getOneYearAgo() != null) measurement.setHistoricalComparisonOneYearAgo(historicalComparison.getOneYearAgo());
        }

        return measurement;
    }

    public MarketIndexIndices convertToModel(MarketIndexIndicesMeasurement measurement) {
        try {
            return MarketIndexIndices.builder()
                .key(measurement.getKey())
                .index(measurement.getIndex())
                .indexSymbol(measurement.getIndexSymbol())
                .timestamp(LocalDateTime.ofInstant(measurement.getTime(), ZoneOffset.UTC))
                .marketData(MarketData.builder()
                    .open(measurement.getMarketDataOpen())
                    .previousClose(measurement.getMarketDataPreviousClose())
                    .high(measurement.getMarketDataHigh())
                    .low(measurement.getMarketDataLow())
                    .last(measurement.getMarketDataClose())
                    .percentChange(measurement.getMarketDataPercentageChange())
                    .build())
                .fundamentalRatios(FundamentalRatios.builder()
                    .priceToEarningRation(measurement.getFundamentalPriceEarning())
                    .priceToBookRation(measurement.getFundamentalPriceBook())
                    .dividenYield(measurement.getFundamentalDividendYield())
                    .build())
                .marketBreadth(MarketBreadth.builder()
                    .advances(measurement.getMarketBreadthAdvances() != null ? measurement.getMarketBreadthAdvances().toString() : "0")
                    .declines(measurement.getMarketBreadthDeclines() != null ? measurement.getMarketBreadthDeclines().toString() : "0")
                    .unchanged(measurement.getMarketBreadthUnchanged() != null ? measurement.getMarketBreadthUnchanged().toString() : "0")
                    .build())
                .historicalComparison(HistoricalComparison.builder()
                    .value(measurement.getHistoricalComparisonValue())
                    .perChange365d(measurement.getHistoricalComparisonPerChange365d())
                    .date365dAgo(parseDate(measurement.getHistoricalComparisonDate365dAgo()))
                    .perChange30d(measurement.getHistoricalComparisonPerChange30d())
                    .date30dAgo(parseDate(measurement.getHistoricalComparisonDate30dAgo()))
                    .previousDay(measurement.getHistoricalComparisonPreviousDay())
                    .oneWeekAgo(measurement.getHistoricalComparisonOneWeekAgo())
                    .oneMonthAgo(measurement.getHistoricalComparisonOneMonthAgo())
                    .oneYearAgo(measurement.getHistoricalComparisonOneYearAgo())
                    .build())
                .build();
        } catch (Exception e) {
            logger.error("Failed to convert measurement to model", e);
            throw e;
        }
    }

    private LocalDateTime parseDate(String date) {
        if (date == null || date.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.of(LocalDateTime.parse(date, DATE_FORMATTER).toLocalDate(), LocalDateTime.now().toLocalTime());
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse date: {}, using current date", date);
            return LocalDateTime.now();
        }
    }
}
