package com.am.common.investment.model.equity.research;

import java.time.LocalDate;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockDeals {
    /** Date of the block deal */
    private LocalDate date;
    
    /** Name of the client involved in the deal */
    private String clientName;
    
    /** Buy/Sell indicator (B/S) */
    private String buySell;
    
    /** Quantity of shares traded */
    private Double quantityShares;
    
    /** Average price per share */
    private Double averagePrice;
    
    /** Stock exchange where the deal took place */
    private String exchange;
}
