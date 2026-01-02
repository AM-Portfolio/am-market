package com.am.common.investment.persistence.document.stock.financial.balancesheet;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.am.common.investment.model.equity.financial.balancesheet.BalanceSheet;
import com.am.common.investment.persistence.document.BaseDocument;
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
@Document(collection = "balance_sheet")
public class BalanceSheetDocument extends BaseDocument{
    private List<BalanceSheet> balanceSheet;
}
