package com.am.common.investment.model.equity.financial.cashflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import com.am.common.investment.model.equity.financial.BaseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockCashFlow extends BaseModel{
    private List<CashFlow> cashFlow;
}
