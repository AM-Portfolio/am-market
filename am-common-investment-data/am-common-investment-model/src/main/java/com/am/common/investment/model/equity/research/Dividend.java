package com.am.common.investment.model.equity.research;

import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dividend {
    /** Type of dividend (e.g., "Final", "Interim") */
    private String dividendType;
    
    /** Date when the dividend was announced */
    private LocalDate announcementDate;
    
    /** Record date for dividend eligibility */
    private LocalDate recordDate;
    
    /** Dividend amount per share */
    private Double divAmount;
    
    /** Dividend percentage */
    private Double divPercentage;
    
    /** Ex-dividend date */
    private LocalDate exDate;
}
