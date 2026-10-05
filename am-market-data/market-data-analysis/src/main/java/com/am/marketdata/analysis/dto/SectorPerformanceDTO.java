package com.am.marketdata.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorPerformanceDTO {
    private String sector;
    private Double change;
    private Integer stockCount;
    private String status;
}
