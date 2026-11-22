package com.am.common.investment.persistence.repository.measurement.impl;

import com.am.common.investment.persistence.influx.measurement.MarketIndexIndicesMeasurement;
import com.am.common.investment.persistence.influx.MarketDataStorageConstants;
import com.am.common.investment.persistence.repository.measurement.MarketIndexIndicesRepository;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MarketIndexIndicesRepositoryImpl implements MarketIndexIndicesRepository {
    private static final Logger logger = LoggerFactory.getLogger(MarketIndexIndicesRepositoryImpl.class);
    private static final String MEASUREMENT_NAME = MarketDataStorageConstants.INDIAN_INDEX_MEASUREMENT;

    @Value("${spring.influx.bucket}")
    private String bucket;

    @Value("${spring.influx.org}")
    private String org;

    private final InfluxDBClient influxDBClient;

    @Override
    public void save(MarketIndexIndicesMeasurement measurement) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi(WriteOptions.builder().batchSize(1).build())) {
            logger.debug("Saving measurement to InfluxDB: measurement=market_index, index={}, key={}, time={}", 
                measurement.getIndex(), measurement.getKey(), measurement.getTime());

            Point point = Point.measurement(MEASUREMENT_NAME)
                .addTag("key", measurement.getKey())
                .addTag("index", measurement.getIndex())
                .addTag("indexSymbol", measurement.getIndexSymbol())
                .addField("market_data_open", measurement.getMarketDataOpen())
                .addField("market_data_previous_close", measurement.getMarketDataPreviousClose())
                .addField("market_data_high", measurement.getMarketDataHigh())
                .addField("market_data_low", measurement.getMarketDataLow())
                .addField("market_data_close", measurement.getMarketDataClose())
                .addField("market_data_percentage_change", measurement.getMarketDataPercentageChange())
                .time(measurement.getTime(), WritePrecision.NS);
            
            logger.debug("Writing measurement to InfluxDB: {}", point.toString());
            writeApi.writePoint(bucket, org, point);
            writeApi.flush();
            logger.debug("Successfully wrote measurement to InfluxDB");
        } catch (Exception e) {
            logger.error("Failed to write measurement to InfluxDB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save market index measurement", e);
        }
    }

    @Override
    public void saveAll(List<MarketIndexIndicesMeasurement> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            return;
        }
        try (WriteApi writeApi = influxDBClient.makeWriteApi(WriteOptions.builder().batchSize(500).build())) {
            for (MarketIndexIndicesMeasurement measurement : measurements) {
                Point point = Point.measurement(MEASUREMENT_NAME)
                        .addTag("key", measurement.getKey())
                        .addTag("index", measurement.getIndex())
                        .addTag("indexSymbol", measurement.getIndexSymbol())
                        .addField("market_data_open", measurement.getMarketDataOpen())
                        .addField("market_data_previous_close", measurement.getMarketDataPreviousClose())
                        .addField("market_data_high", measurement.getMarketDataHigh())
                        .addField("market_data_low", measurement.getMarketDataLow())
                        .addField("market_data_close", measurement.getMarketDataClose())
                        .addField("market_data_percentage_change", measurement.getMarketDataPercentageChange())
                        .time(measurement.getTime(), WritePrecision.NS);
                writeApi.writePoint(bucket, org, point);
            }
            writeApi.flush();
            logger.info("Saved {} market_index measurements in one batch", measurements.size());
        } catch (Exception e) {
            logger.error("Failed to batch-write market index measurements: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to batch-save market index measurements", e);
        }
    }

    @Override
    public List<MarketIndexIndicesMeasurement> findByKey(String key) {
        String query = buildKeyQuery(bucket, MEASUREMENT_NAME, key);

        logger.debug("Executing findByKey query: measurement=market_index, key={}", key);
        logger.debug("Query: {}", query);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<MarketIndexIndicesMeasurement> results = queryApi.query(query, MarketIndexIndicesMeasurement.class);
        logger.debug("Found {} results for key={}", results.size(), key);

        if (!results.isEmpty()) {
            // Set the tags since they are not included in the pivot result
            results.forEach(m -> {
                m.setKey(key);
                m.setIndex(m.getIndex());
                m.setIndexSymbol(m.getIndexSymbol());
            });
        }

        return results;
    }

    @Override
    public List<MarketIndexIndicesMeasurement> findByIndexSymbolAndTimeBetween(String indexSymbol, Instant startTime, Instant endTime) {
        String query = buildHistoryQuery(bucket, MEASUREMENT_NAME, indexSymbol, startTime, endTime);

        logger.debug("Executing findByIndexSymbolAndTimeBetween query: measurement=market_index, indexSymbol={}, start: {}, end: {}", indexSymbol, startTime, endTime);
        logger.debug("Query: {}", query);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<MarketIndexIndicesMeasurement> results = queryApi.query(query, MarketIndexIndicesMeasurement.class);
        logger.debug("Found {} results for indexSymbol={}", results.size(), indexSymbol);

        if (!results.isEmpty()) {
            results.forEach(m -> {
                m.setIndexSymbol(indexSymbol);
                m.setIndex(m.getIndex());
                m.setKey(m.getKey());
            });
        }

        return results;
    }

    /**
     * Resolves equivalent alias tags for Indian benchmark indices.
     * Upstox streams using derivative abbreviations (e.g. "NIFTY", "BANKNIFTY")
     * while the UI and exchange use standard names (e.g. "NIFTY 50", "NIFTY BANK").
     * Querying both ensures zero data loss and full backward compatibility.
     */
    static List<String> getIndexAliases(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }
        String clean = symbol.trim().toUpperCase(java.util.Locale.ROOT);
        // Remove common exchange prefixes if present
        for (String prefix : List.of("NSE_INDEX|", "NSE_INDEX:", "BSE_INDEX|", "BSE_INDEX:", "NSE:", "BSE:")) {
            if (clean.startsWith(prefix)) {
                clean = clean.substring(prefix.length()).trim();
                break;
            }
        }

        return switch (clean) {
            case "NIFTY", "NIFTY 50", "NIFTY50" -> List.of("NIFTY 50", "NIFTY");
            case "BANKNIFTY", "NIFTY BANK", "NIFTYBANK" -> List.of("NIFTY BANK", "BANKNIFTY");
            case "FINNIFTY", "NIFTY FIN SERVICES", "NIFTY FIN SERVICE", "NIFTY FINANCIAL SERVICES" ->
                    List.of("NIFTY FIN SERVICES", "FINNIFTY");
            case "MIDCPNIFTY", "NIFTY MIDCAP 50", "NIFTY MID SELECT" ->
                    List.of("NIFTY MIDCAP 50", "MIDCPNIFTY", "NIFTY MID SELECT");
            default -> List.of(clean);
        };
    }

    /**
     * Builds the Flux query to retrieve historical index points from InfluxDB.
     * Uses a multi-alias tag filter (e.g. r.indexSymbol == "NIFTY 50" or r.indexSymbol == "NIFTY")
     * so that both live streaming ticks and historical 1-minute bars are retrieved in a single pass.
     */
    static String buildHistoryQuery(String bucket, String measurementName, String indexSymbol, Instant startTime, Instant endTime) {
        List<String> aliases = getIndexAliases(indexSymbol);
        String symbolFilter;
        if (aliases.isEmpty()) {
            symbolFilter = String.format("r.indexSymbol == \"%s\"", indexSymbol);
        } else if (aliases.size() == 1) {
            symbolFilter = String.format("r.indexSymbol == \"%s\"", aliases.get(0));
        } else {
            symbolFilter = aliases.stream()
                    .map(a -> String.format("r.indexSymbol == \"%s\"", a))
                    .collect(java.util.stream.Collectors.joining(" or "));
        }

        return String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"%s\") " +
            "|> filter(fn: (r) => %s) " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") " +
            "|> group(columns: []) " +
            "|> sort(columns: [\"_time\"]) ",
            bucket, startTime, endTime, measurementName, symbolFilter
        );
    }

    /**
     * Builds the Flux query for findByKey using alias support.
     */
    static String buildKeyQuery(String bucket, String measurementName, String key) {
        List<String> aliases = getIndexAliases(key);
        String keyFilter;
        if (aliases.isEmpty()) {
            keyFilter = String.format("r.key == \"%s\"", key);
        } else if (aliases.size() == 1) {
            keyFilter = String.format("r.key == \"%s\"", aliases.get(0));
        } else {
            keyFilter = aliases.stream()
                    .map(a -> String.format("r.key == \"%s\"", a))
                    .collect(java.util.stream.Collectors.joining(" or "));
        }

        return String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -30d) " +
            "|> filter(fn: (r) => r._measurement == \"%s\") " +
            "|> filter(fn: (r) => %s) " +
            "|> pivot(rowKey: [\"_time\"], " +
            "        columnKey: [\"_field\"], " +
            "        valueColumn: \"_value\") " +
            "|> last()",
            bucket, measurementName, keyFilter
        );
    }
}
