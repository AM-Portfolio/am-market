package com.am.common.investment.persistence.repository.measurement.impl;

import com.am.common.investment.persistence.influx.measurement.GlobalIndexMeasurement;
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

/**
 * InfluxDB repository for the {@code global_market_index} measurement.
 *
 * <p>This repository is strictly isolated from the Indian index repository
 * ({@code MarketIndexIndicesRepositoryImpl}) which uses the {@code market_index} measurement.
 * All reads and writes here target {@code global_market_index} ONLY.
 *
 * <p><b>UTC Alignment:</b> All timestamps are stored in UTC. The Flux
 * {@code aggregateWindow} queries use a per-exchange offset (from
 * {@link com.am.common.investment.persistence.document.global.GlobalIndexConfigDocument#getInfluxAggregateOffset()})
 * to align daily candle boundaries to the exchange's trading session,
 * preventing US/European candles from being split across UTC midnight.
 *
 * <p><b>Empty Bucket Prevention:</b> All aggregation queries use
 * {@code createEmpty: false} in Flux to avoid null data points in the
 * response during sessions with low tick frequency (global indices can
 * go minutes between ticks when volume is thin).
 */
@Repository
@RequiredArgsConstructor
public class GlobalIndexInfluxRepository {

    private static final Logger logger = LoggerFactory.getLogger(GlobalIndexInfluxRepository.class);

    /** InfluxDB measurement name — must NOT match the Indian index measurement "market_index". */
    private static final String MEASUREMENT_NAME = "global_market_index";

    @Value("${spring.influx.bucket}")
    private String bucket;

    @Value("${spring.influx.org}")
    private String org;

    private final InfluxDBClient influxDBClient;

    /**
     * Persists a single global index tick (from WebSocket or REST fallback) to InfluxDB.
     *
     * <p>Called by {@code GlobalIndexCacheWriter} on every incoming WebSocket tick
     * for a {@code GLOBAL_*} instrument key.
     *
     * @param measurement the data point to persist
     */
    public void save(GlobalIndexMeasurement measurement) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi(WriteOptions.builder().batchSize(1).build())) {
            logger.debug("Writing global tick to InfluxDB: measurement={}, instrumentKey={}, time={}",
                    MEASUREMENT_NAME, measurement.getInstrumentKey(), measurement.getTime());

            Point point = buildPoint(measurement);
            writeApi.writePoint(bucket, org, point);
            writeApi.flush();

            logger.debug("Successfully wrote global tick to InfluxDB for instrumentKey={}",
                    measurement.getInstrumentKey());
        } catch (Exception e) {
            logger.error("Failed to write global index measurement to InfluxDB for instrumentKey={}: {}",
                    measurement.getInstrumentKey(), e.getMessage(), e);
            throw new RuntimeException("Failed to save global index measurement", e);
        }
    }

    /**
     * Bulk-inserts a list of historical data points during the admin backfill sync.
     *
     * <p>Called by {@code GlobalHistoricalSyncService} when processing candles from the
     * Upstox Historical Candle API (1 year daily + 30 days 1-minute bars per instrument).
     *
     * <p>Uses a larger batch size for efficiency during bulk writes.
     *
     * @param measurements list of historical data points to persist
     */
    public void saveAll(List<GlobalIndexMeasurement> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            logger.debug("saveAll called with empty list, skipping write.");
            return;
        }

        // Use a larger batch size for bulk historical writes to reduce network round-trips
        try (WriteApi writeApi = influxDBClient.makeWriteApi(WriteOptions.builder()
                .batchSize(500)
                .flushInterval(10_000)
                .build())) {

            logger.info("Bulk-writing {} global index data points to InfluxDB measurement={}",
                    measurements.size(), MEASUREMENT_NAME);

            for (GlobalIndexMeasurement m : measurements) {
                writeApi.writePoint(bucket, org, buildPoint(m));
            }
            writeApi.flush();

            logger.info("Successfully bulk-wrote {} points to InfluxDB", measurements.size());
        } catch (Exception e) {
            logger.error("Failed to bulk-write global index measurements to InfluxDB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to bulk-save global index measurements", e);
        }
    }

    /**
     * Queries historical data for a global index within a time range,
     * aggregated to the requested interval and aligned to the exchange's trading day.
     *
     * <p>Uses {@code createEmpty: false} in the Flux query to suppress empty
     * buckets during thin-volume periods or non-trading hours.
     *
     * @param instrumentKey          the Upstox instrument key (e.g., "GLOBAL_INDEX|DJI")
     * @param startTime              query start time (inclusive), in UTC
     * @param endTime                query end time (inclusive), in UTC
     * @param aggregateWindowEvery   Flux duration string for aggregation interval
     *                               (e.g., "1d", "1h", "5m")
     * @param aggregateOffset        per-exchange UTC offset string from the config document
     *                               (e.g., "-14h30m" for NYSE) to align daily candles to
     *                               the exchange's trading session boundary
     * @return list of aggregated OHLC data points
     */
    public List<GlobalIndexMeasurement> findByInstrumentKeyAndTimeBetween(
            String instrumentKey,
            Instant startTime,
            Instant endTime,
            String aggregateWindowEvery,
            String aggregateOffset) {

        // Flux query notes:
        // - `aggregateWindow` buckets data into fixed intervals; `offset` shifts the bucket
        //   boundary from UTC midnight to the exchange's session open time.
        // - `createEmpty: false` prevents null rows in thin-volume periods.
        // - `pivot` flattens field columns so the Java client can map to the POJO.
        String query = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: %s, stop: %s) " +
            "|> filter(fn: (r) => r._measurement == \"%s\") " +
            "|> filter(fn: (r) => r.instrumentKey == \"%s\") " +
            "|> aggregateWindow(every: %s, offset: %s, fn: mean, createEmpty: false) " +
            "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
            bucket, startTime, endTime, MEASUREMENT_NAME, instrumentKey,
            aggregateWindowEvery, aggregateOffset
        );

        logger.debug("Executing historical query for global index: instrumentKey={}, start={}, end={}, every={}, offset={}",
                instrumentKey, startTime, endTime, aggregateWindowEvery, aggregateOffset);

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<GlobalIndexMeasurement> results = queryApi.query(query, GlobalIndexMeasurement.class);

        logger.debug("Found {} data points for instrumentKey={}", results.size(), instrumentKey);

        // Re-populate tags since InfluxDB pivot drops them from the result rows
        results.forEach(m -> m.setInstrumentKey(instrumentKey));

        return results;
    }

    /**
     * Finds the timestamp of the last recorded data point for the given instrument key.
     * Looks back up to 30 days.
     *
     * @param instrumentKey the Upstox instrument key
     * @return Optional containing the last timestamp if found, or empty
     */
    public java.util.Optional<Instant> findLastTimestamp(String instrumentKey) {
        try {
            String query = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -30d) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +
                "|> filter(fn: (r) => r.instrumentKey == \"%s\") " +
                "|> last() " +
                "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                bucket, MEASUREMENT_NAME, instrumentKey
            );

            logger.debug("Executing last timestamp query for: {}", instrumentKey);
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<GlobalIndexMeasurement> results = queryApi.query(query, GlobalIndexMeasurement.class);

            if (results != null && !results.isEmpty()) {
                return java.util.Optional.ofNullable(results.get(0).getTime());
            }
        } catch (Exception e) {
            logger.error("Failed to query last timestamp for: {}", instrumentKey, e);
        }
        return java.util.Optional.empty();
    }

    /**
     * Builds an InfluxDB {@link Point} from a {@link GlobalIndexMeasurement}.
     * Extracted as a private helper to keep save() and saveAll() DRY.
     *
     * @param m the measurement to convert
     * @return the InfluxDB write point
     */
    private Point buildPoint(GlobalIndexMeasurement m) {
        return Point.measurement(MEASUREMENT_NAME)
                .addTag("instrumentKey", m.getInstrumentKey())
                .addTag("name", m.getName())
                .addTag("segment", m.getSegment())
                .addField("open", m.getOpen())
                .addField("high", m.getHigh())
                .addField("low", m.getLow())
                .addField("close", m.getClose())
                .addField("previous_close", m.getPreviousClose())
                .addField("change_percent", m.getChangePercent())
                .time(m.getTime(), WritePrecision.NS);
    }
}
