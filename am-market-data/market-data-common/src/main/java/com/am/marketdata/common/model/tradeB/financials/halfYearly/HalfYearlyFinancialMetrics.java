package com.am.marketdata.common.model.tradeB.financials.halfYearly;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HalfYearlyFinancialMetrics {
    private Double totalRevenue;
    private Double operatingRevenue;
    private Double totalExpenses;
    private Double otherIncome;
    private Double tax;
    private Double interest;
    private Double employeeCost;
    private Double depreciationAndAmortization;
    private Double profitAfterTax;
    private Double profitBeforeTax;
    private Double adjEpsInRsBasic;
    private Double adjEpsInRsDiluted;
    private Double patMargin;
    private Double operatingExpenses;
    private Double operatingProfit;
    private Double taxPer;
    private Double opmPercentage;
    private Double revenueGrowthPer;
    private Double patGrowth;
    private Double patMarginGrowth;

    @JsonIgnore
    private Object additionalProperties;

    @JsonAnyGetter
    public Object getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties = value;
    }
}
