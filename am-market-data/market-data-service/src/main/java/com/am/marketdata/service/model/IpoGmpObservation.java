package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A provider observation retained for the IPO's GMP history. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoGmpObservation {
    private String provider;
    private String providerSlug;
    private String price;
    private String percentage;
    private String kostak;
    private String subjectToSauda;
    private String estimatedListingPrice;
    private Instant updatedAt;
    private Instant capturedAt;
    private String captureRunId;
}
