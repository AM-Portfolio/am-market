package com.am.common.investment.persistence.document.stock.financial.profitandloss;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.am.common.investment.model.equity.financial.profitandloss.ProfitAndLoss;
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
@Document(collection = "profit_and_loss")
public class ProfitAndLossDocument extends BaseDocument{
    private List<ProfitAndLoss> profitAndLoss;
}
