package com.am.common.investment.model.events;


import java.time.LocalDateTime;
import java.util.List;

import com.am.common.investment.model.equity.ETFIndies;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ETFIndicesPriceUpdateEvent {
    private String eventType;
    private LocalDateTime timestamp;
    private List<ETFIndies> etfIndies;
}
