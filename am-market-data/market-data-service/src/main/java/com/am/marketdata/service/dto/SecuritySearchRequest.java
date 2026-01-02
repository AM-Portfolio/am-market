package com.am.marketdata.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySearchRequest implements Serializable {
    private List<String> symbols;
    private String isin;
    private String sector;
    private String industry;
    private String index; // Index name (e.g., "NIFTY 50")
    private String query; // General text search (symbol or ISIN regex)
}
