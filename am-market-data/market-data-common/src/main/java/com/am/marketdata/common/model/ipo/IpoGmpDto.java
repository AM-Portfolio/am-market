package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpoGmpDto {
    private String provider;
    private String providerSlug;
    private String price;
    private String percentage;
    private String kostak;
    private String subjectToSauda;
    private String estimatedListingPrice;
    private Instant updatedAt;
    private Instant capturedAt;
    private List<IpoGmpObservationDto> history;
}
