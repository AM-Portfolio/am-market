package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Monthly calendar of IPO listing events with freshness metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxIpoCalendarMonthDto {

    /**
     * Calendar year (e.g., 2026).
     */
    private Integer year;

    /**
     * Calendar month (1-12).
     */
    private Integer month;

    /**
     * List of IPO listing entries scheduled or listed in the requested month.
     */
    private List<AsraxIpoCalendarEntryDto> entries;

    /**
     * Freshness state of the returned calendar dataset.
     */
    private IpoCalendarDataFreshness dataFreshness;

    /**
     * ISO-8601 timestamp of last successful complete Upstox sync, nullable if never synced.
     */
    private String lastSuccessfulSyncAt;
}
