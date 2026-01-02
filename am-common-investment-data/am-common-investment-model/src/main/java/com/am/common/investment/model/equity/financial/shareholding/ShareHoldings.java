package com.am.common.investment.model.equity.financial.shareholding;

import com.am.common.investment.model.equity.financial.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class ShareHoldings {

    private String periodEndDate;

    /** Percentage of shares held by promoters */
    private Double promotersHolding;
    
    /** Percentage of shares held by Foreign Institutional Investors */
    private Double fiisHolding;
    
    /** Percentage of shares held by Domestic Institutional Investors */
    private Double diisHolding;
    
    /** Percentage of shares held by public */
    private Double publicHolding;
    
    /** Percentage of shares pledged by promoters */
    private Double pledgeSharePercent;
    
    /** Change in promoter's holding percentage */
    private Double promotersChange;
    
    /** Change in FIIs holding percentage */
    private Double fiisChange;
    
    /** Change in DIIs holding percentage */
    private Double diisChange;
    
    /** Change in pledge share percentage */
    private Double pledgeShareChange;
    
    /** Change in public holding percentage */
    private Double publicHoldingChange;
}
