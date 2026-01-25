package com.am.marketdata.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request parameters for bulk security updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateRequest {

    /**
     * Fields that should be updated. If null or empty, no updates will be performed
     * (dry-run mode)
     * Valid values: "companyName", "sector", "industry", "marketCapValue",
     * "marketCapType", "status", "group"
     */
    private Set<String> fieldsToUpdate;

    /**
     * Strategy for matching securities in the file to existing records
     */
    @Builder.Default
    private MatchingStrategy matchingStrategy = MatchingStrategy.STRICT_SYMBOL;

    /**
     * If true, preview changes without actually saving them
     */
    @Builder.Default
    private boolean dryRun = false;

    /**
     * Matching strategies for finding securities
     */
    public enum MatchingStrategy {
        STRICT_ISIN, // Exact ISIN match (case-sensitive)
        STRICT_SYMBOL, // Exact Symbol match (case-sensitive)
        STRICT_BOTH, // Both Symbol AND ISIN must match
        LOOSE_ISIN, // Case-insensitive ISIN match
        LOOSE_SYMBOL // Case-insensitive Symbol match
    }
}
