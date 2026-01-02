package com.am.common.investment.model.events;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;


@Data
@Builder
public class StockIndicesPriceUpdateEvent {
    private String eventType;
    private LocalDateTime timestamp;
    private StockInsidicesEventData stockIndices;
}