package com.am.common.investment.persistence.influx.measurement;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

/**
 * InfluxDB measurement class for global market index time-series data.
 *
 * <p>Measurement: {@code global_market_index}
 *
 * <p><b>Why a separate measurement from {@code market_index}?</b>
 * <ul>
 *   <li>Indian indices store rich data (market breadth, fundamentals, sector weightage)
 *       that global indices do not have. A shared measurement would create sparse rows
 *       with many null fields, degrading InfluxDB query performance.</li>
 *   <li>Keeping them separate allows us to flush, archive, or backfill global data
 *       independently without any risk to the Indian index time-series.</li>
 * </ul>
 *
 * <p><b>Timezone Handling:</b>
 * All timestamps are stored in UTC ({@code Instant}). When querying for daily OHLC
 * charts, the Flux {@code aggregateWindow} uses the per-exchange {@code influxAggregateOffset}
 * from {@code GlobalIndexConfigDocument} to align bucket boundaries to the exchange's
 * trading day (e.g., NYSE daily candle: 9:30 AM to 4:00 PM EST, not UTC midnight).
 * This prevents US/European candles from being split across two calendar days in UTC.
 *
 * <p><b>Written by:</b> {@code GlobalIndexCacheWriter} (live ticks via WebSocket)
 * and {@code GlobalHistoricalSyncService} (historical backfill via admin sync).
 */
@Data
@Measurement(name = "global_market_index")
public class GlobalIndexMeasurement {

    // -------------------------------------------------------------------------
    // TAGS — indexed in InfluxDB, used for fast filtering
    // -------------------------------------------------------------------------

    /**
     * The Upstox instrument key (e.g., "GLOBAL_INDEX|DJI").
     * Tag for fast symbol-based filtering in Flux queries.
     */
    @Column(tag = true)
    private String instrumentKey;

    /**
     * Human-readable index name (e.g., "Dow Jones Industrial Average").
     * Tag for display and grouped queries.
     */
    @Column(tag = true)
    private String name;

    /**
     * Market segment identifier. Always "GLOBAL" for this measurement.
     * Tag allows multi-measurement queries to filter by segment if needed.
     */
    @Column(tag = true)
    private String segment;

    // -------------------------------------------------------------------------
    // TIMESTAMP — stored in UTC, aligned per-exchange in queries
    // -------------------------------------------------------------------------

    /**
     * UTC timestamp of this data point. Stored with nanosecond precision.
     * See class-level Javadoc for timezone handling details.
     */
    @Column(timestamp = true)
    private Instant time;

    // -------------------------------------------------------------------------
    // PRICE FIELDS — basic OHLC data available from Upstox Global Instruments API
    // -------------------------------------------------------------------------

    /** Opening price of the current session. */
    @Column(name = "open")
    private Double open;

    /** Intraday high price. */
    @Column(name = "high")
    private Double high;

    /** Intraday low price. */
    @Column(name = "low")
    private Double low;

    /** Latest traded/close price. */
    @Column(name = "close")
    private Double close;

    /**
     * Previous session's closing price.
     * Used to compute change% in the response DTO.
     */
    @Column(name = "previous_close")
    private Double previousClose;

    /**
     * Percentage change from previous close.
     * Pre-computed and stored to avoid recalculation on every read.
     */
    @Column(name = "change_percent")
    private Double changePercent;
}
