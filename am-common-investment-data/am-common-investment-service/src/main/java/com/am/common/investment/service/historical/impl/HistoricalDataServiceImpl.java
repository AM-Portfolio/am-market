package com.am.common.investment.service.historical.impl;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.model.historical.mapper.OHLCVTMapper;
import com.am.common.investment.service.EquityService;
import com.am.common.investment.service.historical.HistoricalDataService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the HistoricalDataService that uses EquityService
 * to retrieve and process historical price data.
 */
@Service
@RequiredArgsConstructor
public class HistoricalDataServiceImpl implements HistoricalDataService {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataServiceImpl.class);
    
    private final EquityService equityService;
    
    @Override
    public Optional<HistoricalData> getHistoricalData(String tradingSymbol, Instant fromDate, Instant toDate, String interval) {
        logger.debug("Retrieving historical data for symbol: {}, from: {}, to: {}, interval: {}", 
                tradingSymbol, fromDate, toDate, interval);
        
        long startTime = System.currentTimeMillis();
        
        // Get price history from equity service
        List<EquityPrice> prices = equityService.getPriceHistoryByKey(tradingSymbol, fromDate, toDate);
        
        if (prices.isEmpty()) {
            logger.debug("No historical data found for symbol: {}", tradingSymbol);
            return Optional.empty();
        }
        
        // Process data based on interval if needed
        List<EquityPrice> processedPrices = processDataByInterval(prices, interval);
        
        // Convert to OHLCVTPoint objects
        List<OHLCVTPoint> dataPoints = OHLCVTMapper.toOHLCVTPoints(processedPrices);
        
        // Build the historical data object
        HistoricalData historicalData = buildHistoricalData(tradingSymbol, fromDate, toDate, interval, dataPoints, processedPrices.get(0));
        
        long endTime = System.currentTimeMillis();
        logger.info("Retrieved historical data for symbol: {}, points: {}, duration: {}ms", 
                tradingSymbol, historicalData.getDataPointCount(), (endTime - startTime));
        
        return Optional.of(historicalData);
    }
    
    @Override
    public Optional<HistoricalData> getRecentHistoricalData(String tradingSymbol, int lookbackPeriod, String interval) {
        logger.debug("Retrieving recent historical data for symbol: {}, lookback: {}, interval: {}", 
                tradingSymbol, lookbackPeriod, interval);
        
        Instant toDate = Instant.now();
        Instant fromDate = calculateFromDate(toDate, lookbackPeriod, interval);
        
        return getHistoricalData(tradingSymbol, fromDate, toDate, interval);
    }
    
    /**
     * Processes the raw price data according to the specified interval.
     * Resamples data to match the requested interval by aggregating price points.
     * 
     * @param prices The raw price data
     * @param interval The desired interval
     * @return Processed price data
     */
    private List<EquityPrice> processDataByInterval(List<EquityPrice> prices, String interval) {
        if (prices == null || prices.isEmpty() || interval == null || interval.isEmpty()) {
            return prices;
        }
        
        // Sort prices by time
        Collections.sort(prices, (p1, p2) -> p1.getTime().compareTo(p2.getTime()));
        
        // Parse the interval
        int amount = parseIntervalAmount(interval);
        ChronoUnit unit = parseIntervalUnit(interval);
        
        if (amount <= 0 || unit == null) {
            logger.warn("Invalid interval format: {}. Returning original data.", interval);
            return prices;
        }
        
        // Group data points by interval buckets
        Map<Instant, List<EquityPrice>> buckets = new HashMap<>();
        Instant firstTimestamp = prices.get(0).getTime();
        
        for (EquityPrice price : prices) {
            // Calculate which bucket this price belongs to
            long diffUnits = unit.between(firstTimestamp, price.getTime()) / amount;
            Instant bucketKey = firstTimestamp.plus(diffUnits * amount, unit);
            
            // Add to the appropriate bucket
            buckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(price);
        }
        
        // Aggregate data in each bucket
        List<EquityPrice> result = new ArrayList<>(buckets.size());
        for (Map.Entry<Instant, List<EquityPrice>> entry : buckets.entrySet()) {
            Instant bucketTime = entry.getKey();
            List<EquityPrice> bucketPrices = entry.getValue();
            
            if (!bucketPrices.isEmpty()) {
                result.add(aggregatePrices(bucketPrices, bucketTime));
            }
        }
        
        // Sort the result by time
        Collections.sort(result, (p1, p2) -> p1.getTime().compareTo(p2.getTime()));
        
        logger.debug("Resampled {} raw data points to {} {} intervals", 
                prices.size(), result.size(), interval);
        
        return result;
    }
    
    /**
     * Aggregates a list of price points into a single price point using OHLC method.
     * 
     * @param prices The list of prices to aggregate
     * @param bucketTime The timestamp to use for the aggregated price
     * @return An aggregated price point
     */
    private EquityPrice aggregatePrices(List<EquityPrice> prices, Instant bucketTime) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        
        // Use the first price for reference data
        EquityPrice reference = prices.get(0);
        
        // Find OHLC values
        double open = reference.getOhlcv().getOpen();
        double close = prices.get(prices.size() - 1).getOhlcv().getClose();
        
        double high = prices.stream()
                .mapToDouble(p -> p.getOhlcv().getHigh())
                .max()
                .orElse(0.0);
                
        double low = prices.stream()
                .mapToDouble(p -> p.getOhlcv().getLow())
                .min()
                .orElse(0.0);
                
        long volume = prices.stream()
                .mapToLong(p -> p.getOhlcv().getVolume() != null ? p.getOhlcv().getVolume() : 0L)
                .sum();
        
        // Create the aggregated price
        return EquityPrice.builder()
                .symbol(reference.getSymbol())
                .isin(reference.getIsin())
                .exchange(reference.getExchange())
                .currency(reference.getCurrency())
                .time(bucketTime)
                .ohlcv(OHLCVTPoint.builder()
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume)
                        .build())
                .build();
    }
    
    /**
     * Parses the numeric amount from an interval string.
     * 
     * @param interval The interval string (e.g., "1d", "15m")
     * @return The numeric amount, or 1 if not specified
     */
    private int parseIntervalAmount(String interval) {
        if (interval == null || interval.isEmpty()) {
            return 1;
        }
        
        String numericPart = interval.replaceAll("[^0-9]", "");
        return numericPart.isEmpty() ? 1 : Integer.parseInt(numericPart);
    }
    
    /**
     * Parses the time unit from an interval string.
     * 
     * @param interval The interval string (e.g., "1d", "15m")
     * @return The corresponding ChronoUnit
     */
    private ChronoUnit parseIntervalUnit(String interval) {
        if (interval == null || interval.isEmpty()) {
            return ChronoUnit.DAYS; // Default to days
        }
        
        String unitPart = interval.replaceAll("[0-9]", "");
        
        return switch (unitPart.toLowerCase()) {
            case "m" -> ChronoUnit.MINUTES;
            case "h" -> ChronoUnit.HOURS;
            case "d" -> ChronoUnit.DAYS;
            case "w" -> ChronoUnit.WEEKS;
            case "mo" -> ChronoUnit.MONTHS;
            default -> {
                logger.warn("Unrecognized interval unit: {}. Defaulting to days.", unitPart);
                yield ChronoUnit.DAYS;
            }
        };
    }
    
    /**
     * Builds a HistoricalData object from the processed price data and query parameters.
     * 
     * @param tradingSymbol The trading symbol
     * @param fromDate The start date
     * @param toDate The end date
     * @param interval The interval
     * @param dataPoints The OHLCVT data points
     * @param referencePrice A reference price for metadata
     * @return A HistoricalData object
     */
    private HistoricalData buildHistoricalData(String tradingSymbol, Instant fromDate, Instant toDate, 
                                               String interval, List<OHLCVTPoint> dataPoints, EquityPrice referencePrice) {
        // Extract additional metadata from the reference price
        String exchange = referencePrice != null ? referencePrice.getExchange() : null;
        String currency = referencePrice != null ? referencePrice.getCurrency() : null;
        String isin = referencePrice != null ? referencePrice.getIsin() : null;
        
        return HistoricalData.builder()
                .tradingSymbol(tradingSymbol)
                .isin(isin)
                .fromDate(fromDate)
                .toDate(toDate)
                .interval(interval)
                .dataPoints(dataPoints)
                .dataPointCount(dataPoints.size())
                .exchange(exchange)
                .currency(currency)
                .retrievalTime(Instant.now())
                .build();
    }
    
    /**
     * Calculates the from date based on the to date, lookback period, and interval.
     * 
     * @param toDate The end date
     * @param lookbackPeriod The number of periods to look back
     * @param interval The interval string (e.g., "1d", "1h", "15m")
     * @return The calculated from date
     */
    private Instant calculateFromDate(Instant toDate, int lookbackPeriod, String interval) {
        // Parse the interval string to determine the time unit and amount
        if (interval == null || interval.isEmpty()) {
            // Default to daily if interval is not specified
            return toDate.minus(lookbackPeriod, ChronoUnit.DAYS);
        }
        
        // Extract the numeric part and the unit part
        String numericPart = interval.replaceAll("[^0-9]", "");
        String unitPart = interval.replaceAll("[0-9]", "");
        
        int amount = numericPart.isEmpty() ? 1 : Integer.parseInt(numericPart);
        
        ChronoUnit unit;
        switch (unitPart.toLowerCase()) {
            case "m":
                unit = ChronoUnit.MINUTES;
                break;
            case "h":
                unit = ChronoUnit.HOURS;
                break;
            case "d":
                unit = ChronoUnit.DAYS;
                break;
            case "w":
                unit = ChronoUnit.WEEKS;
                break;
            case "mo":
                unit = ChronoUnit.MONTHS;
                break;
            default:
                logger.warn("Unrecognized interval unit: {}. Defaulting to days.", unitPart);
                unit = ChronoUnit.DAYS;
                break;
        }
        
        // Calculate the total duration
        return toDate.minus(lookbackPeriod * amount, unit);
    }

    @Override
    public Optional<HistoricalData> saveHistoricalData(HistoricalData historicalData) {
        if (historicalData == null || historicalData.getDataPoints() == null || historicalData.getDataPoints().isEmpty()) {
            logger.warn("Cannot save historical data: null or empty data points");
            return Optional.empty();
        }
        
        logger.info("Saving historical data for symbol: {}, points: {}", 
                historicalData.getTradingSymbol(), historicalData.getDataPointCount());
        long startTime = System.currentTimeMillis();
        
        try {
            // Convert OHLCVTPoints to EquityPrice objects
            List<EquityPrice> equityPrices = OHLCVTMapper.toEquityPrices(
                historicalData.getDataPoints(),
                historicalData.getTradingSymbol(),
                historicalData.getIsin(),
                historicalData.getExchange(),
                historicalData.getCurrency()
            );
            
            // Save all prices using the equity service
            equityService.saveAllPrices(equityPrices);
            
            long endTime = System.currentTimeMillis();
            logger.info("Successfully saved {} data points for symbol: {}, duration: {}ms", 
                    equityPrices.size(), historicalData.getTradingSymbol(), (endTime - startTime));
            
            return Optional.of(historicalData);
        } catch (Exception e) {
            logger.error("Error saving historical data for symbol: {}", historicalData.getTradingSymbol(), e);
            return Optional.empty();
        }
    }
}