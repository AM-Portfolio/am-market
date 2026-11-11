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
import java.util.Comparator;
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
    private final com.am.common.investment.service.MarketIndexIndicesService marketIndexIndicesService;
    private final com.am.common.investment.persistence.repository.measurement.impl.GlobalIndexInfluxRepository globalIndexInfluxRepository;
    private final com.am.common.investment.persistence.document.global.GlobalIndexConfigRepository globalIndexConfigRepository;
    
    @Override
    public Optional<HistoricalData> getHistoricalData(String tradingSymbol, Instant fromDate, Instant toDate, String interval) {
        return getHistoricalData(tradingSymbol, fromDate, toDate, interval, false);
    }

    @Override
    public Optional<HistoricalData> getHistoricalData(String tradingSymbol, Instant fromDate, Instant toDate, String interval, boolean isIndexSymbol) {
        logger.debug("Retrieving historical data for symbol: {}, from: {}, to: {}, interval: {}, isIndexSymbol: {}", 
                tradingSymbol, fromDate, toDate, interval, isIndexSymbol);
        
        long startTime = System.currentTimeMillis();
        
        List<EquityPrice> prices;
        if (isIndexSymbol) {
            if (isGlobalIndexKey(tradingSymbol)) {
                // Determine Flux aggregation interval
                String aggregateWindowEvery;
                if ("DAY".equalsIgnoreCase(interval) || "1D".equalsIgnoreCase(interval)) {
                    aggregateWindowEvery = "1d";
                } else if ("WEEK".equalsIgnoreCase(interval) || "1W".equalsIgnoreCase(interval)) {
                    aggregateWindowEvery = "1w";
                } else if ("MONTH".equalsIgnoreCase(interval) || "1M".equalsIgnoreCase(interval)) {
                    aggregateWindowEvery = "30d";
                } else if ("1m".equalsIgnoreCase(interval) || "MINUTE".equalsIgnoreCase(interval)) {
                    aggregateWindowEvery = "1m";
                } else if ("5m".equalsIgnoreCase(interval) || "FIVE_MINUTE".equalsIgnoreCase(interval)) {
                    aggregateWindowEvery = "5m";
                } else if ("1H".equalsIgnoreCase(interval) || "HOUR".equalsIgnoreCase(interval)) {
                    aggregateWindowEvery = "1h";
                } else {
                    aggregateWindowEvery = "1d";
                }

                // Resolve exchange offset
                String pureSymbol = tradingSymbol.replace("GLOBAL_INDEX|", "");
                Optional<com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument> configOpt = 
                        globalIndexConfigRepository.findBySymbol(pureSymbol);
                String offset = configOpt.map(com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument::getInfluxAggregateOffset).orElse("+0h");

                logger.info("Querying global index historical data for symbol={} key={} every={} offset={}", 
                        tradingSymbol, tradingSymbol, aggregateWindowEvery, offset);

                List<com.am.common.investment.persistence.influx.measurement.GlobalIndexMeasurement> dbMeasurements = 
                        globalIndexInfluxRepository.findByInstrumentKeyAndTimeBetween(tradingSymbol, fromDate, toDate, aggregateWindowEvery, offset);

                prices = dbMeasurements.stream().map(m -> {
                    com.am.common.investment.model.historical.OHLCVTPoint ohlcv = com.am.common.investment.model.historical.OHLCVTPoint.builder()
                            .open(m.getOpen() != null ? m.getOpen() : 0.0)
                            .high(m.getHigh() != null ? m.getHigh() : 0.0)
                            .low(m.getLow() != null ? m.getLow() : 0.0)
                            .close(m.getClose() != null ? m.getClose() : 0.0)
                            .volume(0L)
                            .build();

                    return com.am.common.investment.model.equity.EquityPrice.builder()
                            .symbol(tradingSymbol)
                            .time(m.getTime())
                            .lastPrice(m.getClose() != null ? m.getClose() : 0.0)
                            .ohlcv(ohlcv)
                            .exchange("GLOBAL")
                            .currency("USD")
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());
            } else {
                List<com.am.common.investment.model.equity.MarketIndexIndices> indices = 
                    marketIndexIndicesService.getByIndexSymbolAndTimeBetween(tradingSymbol, fromDate, toDate);
                
                prices = indices.stream().map(index -> {
                    com.am.common.investment.model.historical.OHLCVTPoint ohlcv = null;
                    if (index.getMarketData() != null) {
                        ohlcv = com.am.common.investment.model.historical.OHLCVTPoint.builder()
                            .open(index.getMarketData().getOpen() != null ? index.getMarketData().getOpen() : 0.0)
                            .high(index.getMarketData().getHigh() != null ? index.getMarketData().getHigh() : 0.0)
                            .low(index.getMarketData().getLow() != null ? index.getMarketData().getLow() : 0.0)
                            .close(index.getMarketData().getLast() != null ? index.getMarketData().getLast() : 0.0)
                            .volume(0L)
                            .build();
                    }
                    
                    return com.am.common.investment.model.equity.EquityPrice.builder()
                        .symbol(index.getIndexSymbol())
                        .time(index.getTimestamp() != null ? index.getTimestamp().toInstant(java.time.ZoneOffset.UTC) : null)
                        .lastPrice(index.getMarketData() != null && index.getMarketData().getLast() != null ? index.getMarketData().getLast() : 0.0)
                        .ohlcv(ohlcv)
                        .exchange("NSE")
                        .currency("INR")
                        .build();
                })
                .filter(price -> price.getTime() != null && price.getOhlcv() != null)
                .collect(java.util.stream.Collectors.toList());
            }
        } else {
            // Get price history from equity service using native DB-level window aggregation
            prices = equityService.getPriceHistoryByKey(tradingSymbol, fromDate, toDate, interval);
        }

        if (prices.isEmpty()) {
            logger.debug("No historical data found for symbol: {}", tradingSymbol);
            return Optional.empty();
        }
        
        // Indian market_index reads are raw and have no aggregateWindow. Always
        // resample them here. Global-index and equity repositories already apply
        // their own query aggregation for non-empty intervals.
        boolean rawIndianIndex = isIndexSymbol && !isGlobalIndexKey(tradingSymbol);
        List<EquityPrice> processedPrices;
        if (rawIndianIndex || interval == null || interval.isEmpty()) {
            processedPrices = processDataByInterval(prices, interval, rawIndianIndex);
        } else {
            logger.debug("Data pre-aggregated via InfluxDB aggregateWindow for interval {}; bypassing Java processDataByInterval", interval);
            processedPrices = prices;
        }
        
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
    public Map<String, HistoricalData> getHistoricalDataBatch(List<String> tradingSymbols, Instant fromDate,
            Instant toDate, String interval, boolean isIndexSymbol) {
        if (tradingSymbols == null || tradingSymbols.isEmpty()) return Collections.emptyMap();
        if (isIndexSymbol) {
            // Index history uses separate sources and identity rules. Preserve its
            // established path until those stores expose a compatible batch query.
            return HistoricalDataService.super.getHistoricalDataBatch(
                    tradingSymbols, fromDate, toDate, interval, true);
        }

        List<EquityPrice> prices = equityService.getPriceHistoryBySymbols(
                tradingSymbols, fromDate, toDate, interval);
        if (prices == null || prices.isEmpty()) return Collections.emptyMap();

        Map<String, List<EquityPrice>> bySymbol = new HashMap<>();
        for (EquityPrice price : prices) {
            if (price == null || price.getSymbol() == null || price.getTime() == null || price.getOhlcv() == null) continue;
            String cleanSym = price.getSymbol().replaceAll("(?i)^(NSE_EQ\\||NSE_INDEX\\||BSE_EQ\\||BSE_INDEX\\||NSE_EQ:|BSE_EQ:|NSE:|BSE:)", "").trim();
            bySymbol.computeIfAbsent(cleanSym, ignored -> new ArrayList<>()).add(price);
        }

        Map<String, HistoricalData> result = new HashMap<>();
        for (Map.Entry<String, List<EquityPrice>> entry : bySymbol.entrySet()) {
            List<EquityPrice> symbolPrices = entry.getValue();
            symbolPrices.sort(java.util.Comparator.comparing(EquityPrice::getTime));
            List<OHLCVTPoint> dataPoints = OHLCVTMapper.toOHLCVTPoints(symbolPrices);
            if (dataPoints == null || dataPoints.isEmpty()) continue;
            result.put(entry.getKey(), buildHistoricalData(entry.getKey(), fromDate, toDate,
                    interval, dataPoints, symbolPrices.get(0)));
        }
        return result;
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
    private List<EquityPrice> processDataByInterval(List<EquityPrice> prices, String interval,
            boolean rawIndianIndex) {
        if (prices == null || prices.isEmpty() || interval == null || interval.isEmpty()) {
            return prices;
        }
        
        // Repository results may be immutable or split across unordered series.
        // Sort a local copy so OHLC open/close samples are chronological.
        List<EquityPrice> orderedPrices = prices.stream()
                .filter(price -> price != null && price.getTime() != null)
                .sorted(Comparator.comparing(EquityPrice::getTime))
                .toList();
        if (orderedPrices.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Parse the interval
        int amount = parseIntervalAmount(interval);
        ChronoUnit unit = parseIntervalUnit(interval);
        
        if (amount <= 0 || unit == null) {
            logger.warn("Invalid interval format: {}. Returning original data.", interval);
            return prices;
        }

        if (rawIndianIndex && unit.isTimeBased()) {
            orderedPrices = removeConflictingIndexSamplesAtSameTime(orderedPrices, interval);
            if (orderedPrices.isEmpty()) {
                return Collections.emptyList();
            }
        }
        
        // Group data points by interval buckets
        Map<Instant, List<EquityPrice>> buckets = new HashMap<>();
        Instant firstTimestamp = orderedPrices.get(0).getTime();
        
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.ZonedDateTime firstZoned = firstTimestamp.atZone(zone);
        boolean isDateUnit = !unit.isTimeBased();
        
        for (EquityPrice price : orderedPrices) {
            Instant bucketKey;
            if (rawIndianIndex) {
                bucketKey = getIndianIndexBucketKey(price.getTime(), amount, unit);
                if (bucketKey == null) {
                    continue;
                }
            } else if (isDateUnit) {
                long diffUnits = unit.between(firstZoned.toLocalDate(), price.getTime().atZone(zone).toLocalDate()) / amount;
                java.time.LocalDate bucketDate = firstZoned.toLocalDate().plus(diffUnits * amount, unit);
                bucketKey = bucketDate.atTime(firstZoned.toLocalTime()).atZone(zone).toInstant();
            } else {
                long diffUnits = unit.between(firstTimestamp, price.getTime()) / amount;
                bucketKey = firstTimestamp.plus(diffUnits * amount, unit);
            }
            
            // Add to the appropriate bucket
            buckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(price);
        }
        
        // Aggregate data in each bucket
        List<EquityPrice> result = new ArrayList<>(buckets.size());
        for (Map.Entry<Instant, List<EquityPrice>> entry : buckets.entrySet()) {
            Instant bucketTime = entry.getKey();
            List<EquityPrice> bucketPrices = entry.getValue();
            
            if (!bucketPrices.isEmpty()) {
                boolean useIndexPriceSamples = rawIndianIndex && unit.isTimeBased();
                EquityPrice aggregated = useIndexPriceSamples
                        ? aggregateIndianIndexPriceSamples(bucketPrices, bucketTime)
                        : aggregatePrices(bucketPrices, bucketTime);
                if (aggregated != null) {
                    result.add(aggregated);
                }
            }
        }
        
        // Sort the result by time
        Collections.sort(result, (p1, p2) -> p1.getTime().compareTo(p2.getTime()));
        
        logger.debug("Resampled {} raw data points to {} {} intervals", 
                prices.size(), result.size(), interval);
        
        return result;
    }

    /**
     * Influx can return same-time rows from separate tag series for one index.
     * Identical prices are redundant; conflicting prices have no source identity
     * in the existing contract, so drop that timestamp instead of choosing an
     * arbitrary row that could create a false OHLC extreme.
     */
    private List<EquityPrice> removeConflictingIndexSamplesAtSameTime(List<EquityPrice> orderedPrices,
            String interval) {
        List<EquityPrice> result = new ArrayList<>(orderedPrices.size());
        int conflictingTimestamps = 0;
        int start = 0;
        while (start < orderedPrices.size()) {
            int end = start + 1;
            Instant timestamp = orderedPrices.get(start).getTime();
            while (end < orderedPrices.size() && timestamp.equals(orderedPrices.get(end).getTime())) {
                end++;
            }

            Map<Long, EquityPrice> uniquePrices = new HashMap<>();
            for (int i = start; i < end; i++) {
                EquityPrice sample = orderedPrices.get(i);
                Double value = getValidIndexPriceSample(sample);
                if (value != null) {
                    uniquePrices.putIfAbsent(Double.doubleToLongBits(value), sample);
                }
            }

            if (uniquePrices.size() == 1) {
                result.add(uniquePrices.values().iterator().next());
            } else if (uniquePrices.size() > 1) {
                conflictingTimestamps++;
            }
            start = end;
        }

        if (conflictingTimestamps > 0) {
            logger.warn("Dropped {} ambiguous duplicate index timestamps symbol={} interval={}",
                    conflictingTimestamps, orderedPrices.get(0).getSymbol(), interval);
        }
        return result;
    }

    /**
     * Index timestamps are persisted as UTC instants representing market-wall
     * clock values; OHLCVTMapper converts them back to UTC LocalDateTime for the
     * existing chart contract. Bucket on that same wall clock and align intraday
     * intervals to the NSE 09:15 session open.
     */
    private Instant getIndianIndexBucketKey(Instant timestamp, int amount, ChronoUnit unit) {
        java.time.LocalDateTime marketWallTime = java.time.LocalDateTime.ofInstant(timestamp,
                java.time.ZoneOffset.UTC);
        java.time.LocalDate date = marketWallTime.toLocalDate();

        if (unit == ChronoUnit.MINUTES || unit == ChronoUnit.HOURS) {
            java.time.LocalTime sessionOpen = java.time.LocalTime.of(9, 15);
            java.time.LocalTime sessionClose = java.time.LocalTime.of(15, 30);
            java.time.LocalTime pointTime = marketWallTime.toLocalTime();
            if (pointTime.isBefore(sessionOpen) || pointTime.isAfter(sessionClose)) {
                return null;
            }

            long bucketMinutes = unit == ChronoUnit.HOURS ? amount * 60L : amount;
            long minutesFromOpen = java.time.Duration.between(sessionOpen, pointTime).toMinutes();
            long lastBucketStart = ((375L - 1L) / bucketMinutes) * bucketMinutes;
            long bucketStart = Math.min((minutesFromOpen / bucketMinutes) * bucketMinutes, lastBucketStart);
            return date.atTime(sessionOpen).plusMinutes(bucketStart).toInstant(java.time.ZoneOffset.UTC);
        }

        java.time.LocalDate bucketDate;
        if (unit == ChronoUnit.DAYS) {
            long epochDay = date.toEpochDay();
            bucketDate = java.time.LocalDate.ofEpochDay(Math.floorDiv(epochDay, amount) * amount);
        } else if (unit == ChronoUnit.WEEKS) {
            java.time.LocalDate monday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(
                    java.time.DayOfWeek.MONDAY));
            long epochWeek = Math.floorDiv(monday.toEpochDay() - 4L, 7L);
            long bucketWeek = Math.floorDiv(epochWeek, amount) * amount;
            bucketDate = java.time.LocalDate.ofEpochDay(bucketWeek * 7L + 4L);
        } else if (unit == ChronoUnit.MONTHS) {
            long monthIndex = (long) date.getYear() * 12L + date.getMonthValue() - 1L;
            long bucketMonth = Math.floorDiv(monthIndex, amount) * amount;
            bucketDate = java.time.LocalDate.of((int) Math.floorDiv(bucketMonth, 12L),
                    (int) Math.floorMod(bucketMonth, 12L) + 1, 1);
        } else if (unit == ChronoUnit.YEARS) {
            int bucketYear = Math.floorDiv(date.getYear(), amount) * amount;
            bucketDate = java.time.LocalDate.of(bucketYear, 1, 1);
        } else {
            logger.warn("Unsupported index interval unit {}; returning raw points", unit);
            return timestamp;
        }

        return bucketDate.atTime(9, 15).toInstant(java.time.ZoneOffset.UTC);
    }

    /** Build sampled intraday OHLC from index last-price observations, not the
     * running day-level open/high/low snapshots stored on each live tick. */
    private EquityPrice aggregateIndianIndexPriceSamples(List<EquityPrice> prices, Instant bucketTime) {
        List<Double> samples = prices.stream()
                .map(this::getValidIndexPriceSample)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (samples.isEmpty()) {
            return null;
        }

        EquityPrice reference = prices.get(0);
        double open = samples.get(0);
        double close = samples.get(samples.size() - 1);
        double high = samples.stream().mapToDouble(Double::doubleValue).max().orElse(close);
        double low = samples.stream().mapToDouble(Double::doubleValue).min().orElse(close);
        return EquityPrice.builder()
                .symbol(reference.getSymbol())
                .isin(reference.getIsin())
                .exchange(reference.getExchange())
                .currency(reference.getCurrency())
                .time(bucketTime)
                .lastPrice(close)
                .ohlcv(OHLCVTPoint.builder().open(open).high(high).low(low).close(close).volume(0L).build())
                .build();
    }

    private Double getValidIndexPriceSample(EquityPrice price) {
        Double value = price.getLastPrice();
        if (value == null && price.getOhlcv() != null) {
            value = price.getOhlcv().getClose();
        }
        return value != null && Double.isFinite(value) && value > 0 ? value : null;
    }

    private boolean isGlobalIndexKey(String symbol) {
        return symbol != null && (symbol.startsWith("GLOBAL_INDEX|") || symbol.startsWith("GLOBAL_INDEX:"));
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
        
        // Find OHLC values with safety null checks to prevent NullPointerExceptions during unboxing
        double open = (reference.getOhlcv() != null && reference.getOhlcv().getOpen() != null)
                ? reference.getOhlcv().getOpen() : 0.0;
        
        double close = 0.0;
        if (!prices.isEmpty()) {
            EquityPrice lastPrice = prices.get(prices.size() - 1);
            if (lastPrice.getOhlcv() != null && lastPrice.getOhlcv().getClose() != null) {
                close = lastPrice.getOhlcv().getClose();
            }
        }
        
        double high = prices.stream()
                .filter(p -> p.getOhlcv() != null && p.getOhlcv().getHigh() != null)
                .mapToDouble(p -> p.getOhlcv().getHigh())
                .max()
                .orElse(0.0);
                
        double low = prices.stream()
                .filter(p -> p.getOhlcv() != null && p.getOhlcv().getLow() != null)
                .mapToDouble(p -> p.getOhlcv().getLow())
                .min()
                .orElse(0.0);
                
        long volume = prices.stream()
                .filter(p -> p.getOhlcv() != null && p.getOhlcv().getVolume() != null)
                .mapToLong(p -> p.getOhlcv().getVolume())
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

        return switch (unitPart) {
            case "m" -> ChronoUnit.MINUTES;
            case "h", "H" -> ChronoUnit.HOURS;
            case "d", "D" -> ChronoUnit.DAYS;
            case "w", "W" -> ChronoUnit.WEEKS;
            case "M", "mo", "MO" -> ChronoUnit.MONTHS;
            case "y", "Y" -> ChronoUnit.YEARS;
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
