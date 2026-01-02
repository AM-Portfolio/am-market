package com.am.common.investment.persistence.repository.measurement.impl;

import com.am.common.investment.persistence.influx.measurement.EquityPriceMeasurement;
import com.am.common.investment.persistence.repository.measurement.EquityPriceMeasurementRepository;
import com.am.common.investment.persistence.config.EquityRangeConfig;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.am.common.investment.persistence.config.InfluxDBConfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EquityPriceMeasurementRepositoryImpl implements EquityPriceMeasurementRepository {

    private static final Logger logger = LoggerFactory.getLogger(EquityPriceMeasurementRepositoryImpl.class);
    private static final String MEASUREMENT_NAME = "equity";
    private static final int BATCH_SIZE = 5000;
    private static final int FLUSH_INTERVAL = 1000; // milliseconds
    
    private final InfluxDBClient influxDBClient;
    private final EquityRangeConfig rangeConfig;
    private final InfluxDBConfig influxDBConfig;

    private Instant parseRange(String range) {
        String value = range.substring(1, range.length() - 1);
        long amount = Long.parseLong(value);
        ChronoUnit unit = switch (range.charAt(range.length() - 1)) {
            case 'h' -> ChronoUnit.HOURS;
            case 'd' -> ChronoUnit.DAYS;
            case 'w' -> ChronoUnit.WEEKS;
            default -> throw new IllegalArgumentException("Unsupported time unit: " + range);
        };
        return Instant.now().minus(amount, unit);
    }

    @Override
    public void save(EquityPriceMeasurement measurement) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi(WriteOptions.builder().batchSize(1).build())) {
            Point point = Point.measurement("equity")
                .addTag("symbol", measurement.getSymbol())
                .addTag("isin", measurement.getIsin())
                .addTag("exchange", measurement.getExchange())
                .addField("open", measurement.getOpen())
                .addField("high", measurement.getHigh())
                .addField("low", measurement.getLow())
                .addField("close", measurement.getClose())
                .addField("currency", measurement.getCurrency())
                .addField("volume", measurement.getVolume())
                .time(measurement.getTime(), WritePrecision.NS);
            
            System.out.println("Writing point to InfluxDB:");
            System.out.println("  - Measurement: equity");
            System.out.println("  - Tags: symbol=" + measurement.getSymbol() + 
                ", isin=" + measurement.getIsin() + 
                ", exchange=" + measurement.getExchange());
            System.out.println("  - Fields: open=" + measurement.getOpen() + 
                ", high=" + measurement.getHigh() + 
                ", low=" + measurement.getLow() + 
                ", close=" + measurement.getClose() + 
                ", volume=" + measurement.getVolume() + 
                ", currency=" + measurement.getCurrency());
            System.out.println("  - Time: " + measurement.getTime());
            
            writeApi.writePoints(influxDBConfig.getBucket(), influxDBConfig.getOrg(), List.of(point));
            writeApi.flush();
        }
    }

    @Override
    public void saveAll(List<EquityPriceMeasurement> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            logger.warn("No measurements provided to save");
            return;
        }

        logger.debug("Starting batch save of {} equity price measurements", measurements.size());
        long startTime = System.currentTimeMillis();

        try {
            // Configure write options for optimal batch processing
            WriteOptions writeOptions = WriteOptions.builder()
                .batchSize(BATCH_SIZE)
                .flushInterval(FLUSH_INTERVAL)
                .bufferLimit(50_000)
                .build();

            try (WriteApi writeApi = influxDBClient.getWriteApi(writeOptions)) {
                // Write all measurements in a single batch operation
                writeApi.writeMeasurements(WritePrecision.NS, measurements);
                writeApi.flush();
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Log performance metrics
            Map<String, Long> measurementsBySymbol = measurements.stream()
                .collect(Collectors.groupingBy(EquityPriceMeasurement::getSymbol, Collectors.counting()));

            logger.info("Successfully saved {} measurements in {}ms (avg: {:.2f}ms/measurement)", 
                measurements.size(), duration, (double) duration / measurements.size());
            logger.debug("Measurements per symbol: {}", measurementsBySymbol);

        } catch (Exception e) {
            logger.error("Error during batch save of {} measurements: {}", 
                measurements.size(), e.getMessage(), e);
            throw new RuntimeException("Failed to save batch of measurements", e);
        }
    }

    @Override
    public Optional<EquityPriceMeasurement> findLatestByIsin(String isin) {
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s) " +
            "|> filter(fn: (r) => r._measurement == \"equity\") " +
            "|> filter(fn: (r) => r.isin == \"%s\") " +
            "|> last() " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") ",
            influxDBConfig.getBucket(), parseRange(rangeConfig.getDefaultRange()), isin
        );  

        logger.debug("Executing findLatestByIsin query for isin: {}", isin);
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<EquityPriceMeasurement> results = queryApi.query(query, EquityPriceMeasurement.class);
        logger.debug("Found {} results for isin: {}", results.size(), isin);
        
        if (!results.isEmpty()) {
            EquityPriceMeasurement measurement = results.get(0);
            measurement.setIsin(isin);
            measurement.setSymbol(results.get(0).getSymbol());
            measurement.setCurrency(results.get(0).getCurrency());
            measurement.setExchange(results.get(0).getExchange());
            return Optional.of(measurement);
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<EquityPriceMeasurement> findLatestBySymbol(String symbol) {
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s) " +
            "|> filter(fn: (r) => r._measurement == \"equity\") " +
            "|> filter(fn: (r) => r.symbol == \"%s\") " +
            "|> last() " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") ",
            influxDBConfig.getBucket(), parseRange(rangeConfig.getDefaultRange()), symbol
        );

        logger.debug("Executing findLatestBySymbol query for symbol: {}", symbol);
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<EquityPriceMeasurement> results = queryApi.query(query, EquityPriceMeasurement.class);
        logger.debug("Found {} results for symbol: {}", results.size(), symbol);
        
        if (!results.isEmpty()) {
            EquityPriceMeasurement measurement = results.get(0);
            measurement.setSymbol(symbol);
            return Optional.of(measurement);
        }

        
        return Optional.empty();
    }

    @Override
    public List<EquityPriceMeasurement> findBySymbol(String symbol) {
        String query = Flux.from(influxDBConfig.getBucket())
            .range(parseRange(rangeConfig.getHistoryRange()))
            .filter(Restrictions.column("symbol").equal(symbol))
            .toString();

        logger.debug("Executing findBySymbol query for symbol: {}", symbol);
        return influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
    }

    @Override
    public List<EquityPriceMeasurement> findBySymbolAndTimeBetween(String symbol, Instant startTime, Instant endTime) {
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"equity\") " +
            "|> filter(fn: (r) => r.symbol == \"%s\") " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") ",
            influxDBConfig.getBucket(), startTime, endTime, symbol
        );

        logger.debug("Executing findBySymbolAndTimeBetween query for symbol: {}, start: {}, end: {}", 
            symbol, startTime, endTime);
        List<EquityPriceMeasurement> results = influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
        logger.debug("Found {} results", results.size());
        
        results.forEach(measurement -> measurement.setSymbol(symbol));
        return results;
    }

    @Override
    public List<EquityPriceMeasurement> findByIsin(String isin) {
        String query = Flux.from(influxDBConfig.getBucket())
            .range(parseRange(rangeConfig.getHistoryRange()))
            .filter(Restrictions.column("isin").equal(isin))
            .toString();

        logger.debug("Executing findByIsin query for isin: {}", isin);
        return influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
    }

    @Override
    public List<EquityPriceMeasurement> findByIsinAndTimeBetween(String isin, Instant startTime, Instant endTime) {
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"equity\") " +
            "|> filter(fn: (r) => r.isin == \"%s\") " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") ",
            influxDBConfig.getBucket(), startTime, endTime, isin
        );

        logger.debug("Executing findByIsinAndTimeBetween query for isin: {}, start: {}, end: {}", 
            isin, startTime, endTime);
        List<EquityPriceMeasurement> results = influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
        logger.debug("Found {} results", results.size());
        
        results.forEach(measurement -> measurement.setIsin(isin));
        return results;
    }

    @Override
    public List<EquityPriceMeasurement> findByExchange(String exchange) {
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s) " +
            "|> filter(fn: (r) => r._measurement == \"equity\") " +
            "|> filter(fn: (r) => r.exchange == \"%s\") " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") ",
            influxDBConfig.getBucket(), parseRange(rangeConfig.getDefaultRange()), exchange
        );

        logger.debug("Executing findByExchange query for exchange: {}", exchange);
        List<EquityPriceMeasurement> results = influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
        logger.debug("Found {} results for exchange: {}", results.size(), exchange);
        
        results.forEach(measurement -> measurement.setExchange(exchange));
        return results;
    }

    @Override
    public List<EquityPriceMeasurement> findByKeyAndTimeBetween(String key, Instant startTime, Instant endTime) {
        logger.debug("Searching for prices by key: {} between {} and {}", key, startTime, endTime);
        
        // Try by symbol first
        List<EquityPriceMeasurement> bySymbol = findBySymbolAndTimeBetween(key, startTime, endTime);
        if (!bySymbol.isEmpty()) {
            logger.debug("Found {} results by symbol", bySymbol.size());
            return bySymbol;
        }
        
        // If not found by symbol, try by ISIN
        List<EquityPriceMeasurement> byIsin = findByIsinAndTimeBetween(key, startTime, endTime);
        logger.debug("Found {} results by ISIN", byIsin.size());
        return byIsin;
    }
    
    @Override
    public List<EquityPriceMeasurement> findByTradingSymbolIn(List<String> tradingSymbols) {
        if (tradingSymbols == null || tradingSymbols.isEmpty()) {
            logger.warn("Empty or null trading symbols list provided");
            return List.of();
        }
        
        String symbolList = tradingSymbols.stream()
                .map(symbol -> "\"" + symbol + "\"")
                .collect(Collectors.joining(", "));
        
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s) " +
            "|> filter(fn: (r) => r._measurement == \"equity\") " +
            "|> filter(fn: (r) => contains(value: r.symbol, set: [%s])) " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") ",
            influxDBConfig.getBucket(), parseRange(rangeConfig.getDefaultRange()), symbolList
        );

        logger.debug("Executing findByTradingSymbolIn query for symbols: {}", tradingSymbols);
        List<EquityPriceMeasurement> results = influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
        logger.debug("Found {} results for trading symbols", results.size());
        
        return results;
    }
    
    @Override
    public List<EquityPriceMeasurement> findByIsinIn(List<String> isins) {
        if (isins == null || isins.isEmpty()) {
            logger.warn("Empty or null ISINs list provided");
            return List.of();
        }
        
        String isinList = isins.stream()
                .map(isin -> "\"" + isin + "\"")
                .collect(Collectors.joining(", "));
        
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s) " +
            "|> filter(fn: (r) => r._measurement == \"equity\") " +
            "|> filter(fn: (r) => contains(value: r.isin, set: [%s])) " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") ",
            influxDBConfig.getBucket(), parseRange(rangeConfig.getDefaultRange()), isinList
        );

        logger.debug("Executing findByIsinIn query for ISINs: {}", isins);
        List<EquityPriceMeasurement> results = influxDBClient.getQueryApi().query(query, EquityPriceMeasurement.class);
        logger.debug("Found {} results for ISINs", results.size());
        
        return results;
    }
}
