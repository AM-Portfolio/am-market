package com.am.common.investment.model.equity.financial.shareholding;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.am.common.investment.model.equity.financial.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@Document(collection = "stock_share_holdings")
public class StockShareHoldings extends BaseModel{
    private List<ShareHoldings> shareHoldings;
}
