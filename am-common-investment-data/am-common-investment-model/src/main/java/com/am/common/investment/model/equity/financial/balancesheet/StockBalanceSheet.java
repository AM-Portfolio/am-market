package com.am.common.investment.model.equity.financial.balancesheet;

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
public class StockBalanceSheet extends BaseModel{
    private List<BalanceSheet> balanceSheet;
}
