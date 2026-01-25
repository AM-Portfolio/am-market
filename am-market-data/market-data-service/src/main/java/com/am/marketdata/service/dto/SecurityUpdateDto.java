package com.am.marketdata.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for parsed security data from CSV/Excel files
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityUpdateDto {

    // Matching fields
    private String symbol;
    private String isin;

    // Updatable fields (from Equity.csv)
    private String securityCode;
    private String issuerName; // Company name option 1
    private String securityName; // Company name option 2
    private String companyName; // Final resolved company name
    private String sector;
    private String industry;
    private String faceValue;
    private String status; // Active, Delisted, Suspended
    private String group; // A, B, T, X, etc.

    // Market cap (may need parsing if in text format)
    private Long marketCapValue;
    private String marketCapType; // Large Cap, Mid Cap, Small Cap

    // Row number for error reporting
    private int rowNumber;
}
