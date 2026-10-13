package com.am.common.investment.persistence.document.global;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * MongoDB document that stores configuration and sync state for each global market index.
 *
 * <p>Collection: {@code global_index_config}
 *
 * <p>This collection is strictly separate from the Indian index configurations
 * (driven by {@code nseIndicesConfig} YAML) to avoid schema pollution and allow
 * independent management of foreign exchange instrument mappings.
 *
 * <p><b>Seeding:</b> Populated on application startup by {@code GlobalIndexSeeder}
 * if the collection is empty. Also updated daily by a scheduled job that re-downloads
 * the Upstox Global Instruments JSON and diffs it against this collection.
 *
 * <p><b>Backfill Protection:</b> The {@code syncStatus} and {@code lastSyncedAt}
 * fields are used by the admin sync endpoint to prevent double-writes to InfluxDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "global_index_config")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlobalIndexConfigDocument {

    /**
     * Primary key — the human-readable symbol used in API requests.
     * Example: "DJI", "SPX", "FTSE", "N225"
     */
    @Id
    private String symbol;

    /**
     * Display name of the index.
     * Example: "Dow Jones Industrial Average"
     */
    private String name;

    /**
     * The Upstox instrument key used for WebSocket subscriptions and REST API calls.
     * Example: "GLOBAL_INDEX|DJI" (exact format from Upstox Global Instruments JSON).
     *
     * <p>This key is used as the Redis cache key prefix and the InfluxDB tag value.
     */
    @Indexed
    private String instrumentKey;

    /**
     * The exchange/market this index belongs to.
     * Example: "NYSE", "LSE", "TSE", "SSE"
     */
    private String exchange;

    /**
     * The IANA timezone ID for this exchange's local time.
     * Used by {@code GlobalMarketScheduleService} to compute market open/close
     * times in IST with full DST support via {@code java.time.ZoneId}.
     *
     * <p>Example: "America/New_York" for NYSE, "Europe/London" for LSE,
     * "Asia/Tokyo" for TSE.
     */
    private String timezone;

    /**
     * Market open time in the exchange's local timezone (HH:mm format).
     * Example: "09:30" for NYSE (America/New_York)
     */
    private String marketOpenTime;

    /**
     * Market close time in the exchange's local timezone (HH:mm format).
     * Example: "16:00" for NYSE (America/New_York)
     */
    private String marketCloseTime;

    /**
     * The UTC offset string used for aligning InfluxDB {@code aggregateWindow} queries.
     * This ensures daily OHLC candles align to the exchange's trading day, not UTC midnight.
     *
     * <p>Example: "-14h30m" for NYSE (UTC-5 + 9:30 open = 14h30m offset from UTC midnight),
     * or "+5h30m" for NSE (IST).
     *
     * <p>Stored as a string since InfluxDB Flux requires this format directly in queries.
     */
    private String influxAggregateOffset;

    // -------------------------------------------------------------------------
    // ADMIN SYNC STATE — tracks the InfluxDB historical backfill operation
    // Used to prevent double-writes and enable safe retries after partial failures
    // -------------------------------------------------------------------------

    /**
     * Current backfill state for this instrument.
     * See {@link GlobalIndexSyncStatus} for the retry logic.
     */
    @Builder.Default
    private GlobalIndexSyncStatus syncStatus = GlobalIndexSyncStatus.PENDING;

    /**
     * Timestamp of the last successful sync completion.
     * The admin sync endpoint rejects requests if this is within the last 1 hour
     * AND {@code syncStatus == COMPLETE}, unless {@code ?force=true} is passed.
     */
    private Instant lastSyncedAt;
}
