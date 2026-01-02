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
public class CostMetrics {
    private Double totalExpenditure;
    private Double rawMaterialCost;
    private Double manufacturingCost;
    private Double employeeCost;
    private Double interest;
    private Double otherCost;
    private Double operatingExpenses;
    private Double depreciationAndAmortization;
}
