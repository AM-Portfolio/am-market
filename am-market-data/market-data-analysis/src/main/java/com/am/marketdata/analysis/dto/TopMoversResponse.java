package com.am.marketdata.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Named response for the unified Movers request.
 *
 * <p>The JSON stays {"gainers": [...], "losers": [...]}. Naming the response
 * gives OpenAPI and client generation a stable contract without changing users.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopMoversResponse {
    private List<StockMoverDTO> gainers;
    private List<StockMoverDTO> losers;
}
