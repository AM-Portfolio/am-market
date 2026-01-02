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
public class CashFlowOperatingMetrics {
    private Double cashFromOperatingActivities;
    private Double profitFromOperations;
    private Double interestReceived;
    private Double dividendReceived;
    private Double directTaxes;
    private Double exceptionalCfItems;
    
    // Working Capital Changes
    private Double receivables;
    private Double inventory;
    private Double payables;
    private Double workingCapitalChanges;
    private Double otherWcItems;
}
