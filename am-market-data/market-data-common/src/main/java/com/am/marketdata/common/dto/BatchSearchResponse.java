package com.am.marketdata.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch security search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSearchResponse {

    /**
     * Results for each query
     */
    private List<QueryResult> results;

    /**
     * Total number of queries processed
     */
    private Integer totalQueries;

    /**
     * Total number of matches found across all queries
     */
    private Integer totalMatches;

    /**
     * Number of queries with no matches
     */
    private Integer queriesWithNoMatches;

    /**
     * Result for a single query
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryResult {
        /**
         * Original query text
         */
        private String query;

        /**
         * Matching securities
         */
        private List<SecurityMatch> matches;

        /**
         * Number of matches found
         */
        private Integer matchCount;
    }

    /**
     * A matched security with score
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityMatch {
        /**
         * Security symbol
         */
        private String symbol;

        /**
         * Security ISIN
         */
        private String isin;

        /**
         * Company name
         */
        private String companyName;

        /**
         * Sector
         */
        private String sector;

        /**
         * Industry
         */
        private String industry;

        /**
         * Match score (0.0 to 1.0)
         * 1.0 = exact match, < 1.0 = fuzzy match
         */
        private Double matchScore;

        private Long marketCapValue;
        private String marketCapType;

        /**
         * Field that was matched (SYMBOL, ISIN, COMPANY_NAME)
         */
        private String matchedField;
    }
}
