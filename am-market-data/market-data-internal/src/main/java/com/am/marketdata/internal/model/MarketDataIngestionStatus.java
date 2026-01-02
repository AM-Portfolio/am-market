package com.am.marketdata.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "market_data_ingestion_status")
public class MarketDataIngestionStatus {

    @Id
    private String symbol;
    
    private LocalDate lastIngestionDate;
    private LocalDateTime lastUpdateTimestamp;
    private String lastStatus; // SUCCESS / FAILED
}
