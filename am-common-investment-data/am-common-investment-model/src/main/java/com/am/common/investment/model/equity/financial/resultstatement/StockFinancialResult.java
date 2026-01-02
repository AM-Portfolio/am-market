package com.am.common.investment.model.equity.financial.resultstatement;

import java.util.List;

import com.am.common.investment.model.equity.financial.BaseModel;
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
public class StockFinancialResult extends BaseModel {
    private List<FinancialResult> financialResults;
}
