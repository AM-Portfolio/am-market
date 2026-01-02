package com.am.common.investment.model.equity.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiabilitiesMetrics {
    private Double provisions;
    private Double shortTermBorrowings;
    private Double currentLiabilities;
    private Double accountPayables;
    private Double otherCurrentLiabilities;
    private Double nonCurrentLiabilities;
    private Double totalDebits;
}
