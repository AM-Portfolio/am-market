package com.am.marketdata.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;

/**
 * MongoDB document representing the latest previous close snapshot for a security/symbol.
 * Uses _id = symbol for single-record upsert strategy (bounded storage, zero flooding).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "previous_close_snapshots")
public class PreviousCloseDocument implements Serializable {

    @Id
    private String symbol;

    @Field("previous_close")
    private Double previousClose;

    @Field("trade_date")
    private String tradeDate;

    @Field("updated_at")
    private Instant updatedAt;
}
