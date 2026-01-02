package com.am.common.investment.persistence.document.stock.financial.factsheetdividend;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.am.common.investment.model.equity.financial.factsheetdividend.FactSheetDividend;
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
@Document(collection = "fact_sheet_dividend")
public class FactSheetDividendDocument extends BaseDocument{
    private List<FactSheetDividend> factSheetDividend;
}
