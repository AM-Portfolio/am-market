package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified ASRAX domain DTO representing an eligible investor category for an IPO issue.
 * Examples of categories: IND (Retail Individual), HNI (High Net-worth Individual), EMP (Employee), QIB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxInvestorCategoryDto {

    private String category;
    private String description;
    private String subscription;
}
