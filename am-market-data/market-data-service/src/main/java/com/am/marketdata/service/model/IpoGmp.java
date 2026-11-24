package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Current provider GMP plus locally captured history. All fields are optional. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoGmp {
    private String provider;
    private String providerSlug;
    private String price;
    private String percentage;
    private String kostak;
    private String subjectToSauda;
    private String estimatedListingPrice;
    private Instant updatedAt;
    private Instant capturedAt;
    @Builder.Default
    private List<IpoGmpObservation> history = new ArrayList<>();
}
