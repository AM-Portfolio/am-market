package com.am.marketdata.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response for bulk update operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateResponse {

    private int totalRecords;
    private int matchedRecords;
    private int updatedRecords;
    private int failedRecords;
    private List<UpdateResult> results;
    private List<String> errors;

    /**
     * Individual update result for each record
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateResult {
        private String symbol;
        private String isin;
        private MatchStatus status;
        private Map<String, Object> changes; // Field name -> New value
        private String errorMessage;
    }

    /**
     * Status of each update attempt
     */
    public enum MatchStatus {
        MATCHED, // Found matching security
        UPDATED, // Successfully updated
        NOT_FOUND, // No matching security found
        SKIPPED, // Found but no fields to update
        ERROR // Error during processing
    }
}
