package com.am.marketdata.watchlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a custom Watchlist container summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistDto {

    private String id;
    private String userId;
    private String name;
    private Boolean isDefault;
    private Integer displayOrder;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
