package com.am.common.investment.model.equity.research;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkDeals {
    /** Date of the bulk deal */
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
