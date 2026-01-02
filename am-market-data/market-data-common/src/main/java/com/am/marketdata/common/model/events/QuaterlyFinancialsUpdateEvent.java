package com.am.marketdata.common.model.events;

import java.time.LocalDateTime;

import com.am.common.investment.model.equity.financial.resultstatement.QuaterlyResult;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for board of directors data updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuaterlyFinancialsUpdateEvent {
    
    /**
     * Event type
     */
    @JsonProperty("event_type")
    private String eventType;
    
    /**
     * Event timestamp
     */
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;
    
    /**
     * Stock symbol
     */
    @JsonProperty("symbol")
    private String symbol;
    
    /**
     * Quaterly financials information
     */
    @JsonProperty("quaterly_financials_report")
    private QuaterlyResult quaterlyResult;
}
