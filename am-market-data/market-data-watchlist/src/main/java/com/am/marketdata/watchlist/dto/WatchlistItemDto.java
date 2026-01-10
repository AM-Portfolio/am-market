package com.am.marketdata.watchlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistItemDto {
    private String id;
    private String symbol;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
