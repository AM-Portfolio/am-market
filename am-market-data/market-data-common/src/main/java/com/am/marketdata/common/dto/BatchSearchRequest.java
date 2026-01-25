package com.am.marketdata.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch security search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSearchRequest {

    /**
     * List of search queries (company names, symbols, ISINs)
     */
    private List<String> queries;

    /**
     * Maximum results per query (default: 3)
     */
    @Builder.Default
    private Integer limit = 3;

    /**
     * Search fields to match against
     */
    @Builder.Default
    private List<SearchField> searchFields = List.of(
            SearchField.COMPANY_NAME,
            SearchField.SYMBOL,
            SearchField.ISIN);

    /**
     * Minimum match score threshold (0.0 to 1.0)
     */
    @Builder.Default
    private Double minMatchScore = 0.0;

    public enum SearchField {
        COMPANY_NAME,
        SYMBOL,
        ISIN,
        ALL
    }
}
