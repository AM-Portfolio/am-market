package com.am.common.investment.persistence.document.global;

/**
 * Represents the lifecycle state of a historical data backfill operation
 * for a global market index.
 *
 * <p>Used by {@link GlobalIndexConfigDocument} to track the admin sync
 * ({@code POST /v1/market-data/admin/global/sync}) status, ensuring that:
 * <ul>
 *   <li>Duplicate writes to InfluxDB are prevented when status is {@code COMPLETE}.</li>
 *   <li>Retries are always allowed when status is {@code FAILED} or {@code IN_PROGRESS}
 *       (e.g., after a partial sync crash).</li>
 * </ul>
 */
public enum GlobalIndexSyncStatus {

    /**
     * No sync has been attempted yet, or the document was just seeded.
     */
    PENDING,

    /**
     * Sync is currently running. If the server crashes mid-sync,
     * the next startup will see this state and allow an immediate retry.
     */
    IN_PROGRESS,

    /**
     * Sync completed successfully. Requests within the 1-hour cooldown
     * window are rejected unless {@code ?force=true} is passed.
     */
    COMPLETE,

    /**
     * Sync failed partway through. Always allows immediate retry
     * to recover from partial writes.
     */
    FAILED
}
