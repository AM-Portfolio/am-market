package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified ASRAX domain DTO representing summary counts of IPOs categorized by status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxIpoCountsDto {

    /**
     * Number of currently open IPOs.
     */
    private long open;

    /**
     * Number of upcoming IPOs.
     */
    private long upcoming;

    /**
     * Number of closed IPOs.
     */
    private long closed;

    /**
     * Number of open IPOs that are closing today.
     */
    private long closingToday;

    /**
     * Number of listed IPOs.
     */
    private long listed;

    /**
     * Total number of stored IPO documents.
     */
    private long total;

    /**
     * ISO-8601 timestamp of when counts were last calculated/synced.
     */
    private String lastSyncedAt;
}
