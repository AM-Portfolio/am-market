package com.am.marketdata.common.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Kafka event published to the {@code am-previous-close-snapshot} topic by the
 * daily 8:00 AM IST {@code PreviousCloseScheduler}.
 *
 * <p>Each message represents a single symbol's previous-close price across
 * multiple timeframes: 1D, 1W, 1M, 3M, 6M, 1Y, 5Y.</p>
 *
 * <p>Consumers (e.g. am-analysis) should handle {@code null} values in
 * {@code previousCloseValues} gracefully — a null for a given timeframe key
 * indicates data was unavailable (e.g. newly listed symbol without 5Y history).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreviousCloseSnapshotEvent {

    /** Always {@code "Market-Data-Scheduler"} — identifies the originating system. */
    private String source;

    /**
     * Trading symbol used as the unique identifier.
     * Examples: {@code "RELIANCE"}, {@code "NIFTY 50"}, {@code "NIFTY BANK"}.
     */
    private String id;

    /** Display name of the stock or index — same as {@code id}. */
    private String stockName;

    /** ISO-8601 date on which the snapshot was taken, e.g. {@code "2026-06-16"}. */
    private String snapshotDate;

    /**
     * Previous-close price for each timeframe.
     * <ul>
     *   <li>Keys: {@code "1D"}, {@code "1W"}, {@code "1M"}, {@code "3M"},
     *       {@code "6M"}, {@code "1Y"}, {@code "5Y"}</li>
     *   <li>Value is {@code null} when data was unavailable for that timeframe.</li>
     * </ul>
     */
    private Map<String, Double> previousCloseValues;
}
