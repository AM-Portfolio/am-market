package com.am.marketdata.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentSearchCriteria {
    private List<String> queries; // Text search terms (gym balls)
    private List<String> exchanges; // List of exchanges (e.g., NSE, NFO)
    private List<String> instrumentTypes; // List of types (e.g., FUT, CE, PE)
    private List<String> segments; // List of segments (e.g., NSE_EQ, NSE_FO)
    private List<String> isins; // List of ISINs
    private List<String> tradingSymbols; // List of trading symbols
    private Boolean weekly; // Filter by weekly expiry
    private String provider = "UPSTOX"; // Data provider (UPSTOX, ZERODHA, etc.)
}
