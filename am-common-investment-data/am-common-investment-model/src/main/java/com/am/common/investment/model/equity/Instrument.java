package com.am.common.investment.model.equity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing instrument data from exchange
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Instrument {
    private Long instrumentToken;
    private Long exchangeToken;
    private String tradingSymbol;
    private String isin;
    private String name;
    private BigDecimal lastPrice;
    private BigDecimal tickSize;
    private InstrumentType instrumentType;
    private Segment segment;
    private Exchange exchange;
    private String strike;
    private Integer lotSize;
    private Date expiry;
    
    /**
     * Enum representing different types of instruments
     */
    @Getter
    @AllArgsConstructor
    public enum InstrumentType {
        EQUITY("EQ"),
        FUTURE("FUT"),
        PUT_OPTION("PE"),
        CALL_OPTION("CE"),
        INDEX("IDX"),
        UNKNOWN("UNKNOWN");
        
        private final String value;
        
        private static final Map<String, InstrumentType> lookup = new HashMap<>();
        
        static {
            for (InstrumentType type : InstrumentType.values()) {
                lookup.put(type.getValue(), type);
            }
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        public static InstrumentType fromValue(String value) {
            return lookup.getOrDefault(value, UNKNOWN);
        }
    }
    
    /**
     * Enum representing different market segments
     */
    @Getter
    @AllArgsConstructor
    public enum Segment {
        NSE("NSE"),
        BSE("BSE"),
        MCX_OPT("MCX-OPT"),
        NFO_FUT("NFO-FUT"),
        NFO_OPT("NFO-OPT"),
        NCO_FUT("NCO-FUT"),
        NCO_OPT("NCO-OPT"),
        UNKNOWN("UNKNOWN");
        
        private final String value;
        
        private static final Map<String, Segment> lookup = new HashMap<>();
        
        static {
            for (Segment segment : Segment.values()) {
                lookup.put(segment.getValue(), segment);
            }
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        public static Segment fromValue(String value) {
            return lookup.getOrDefault(value, UNKNOWN);
        }
    }
    
    /**
     * Enum representing different exchanges
     */
    @Getter
    @AllArgsConstructor
    public enum Exchange {
        NSE("NSE"),
        BSE("BSE"),
        MCX("MCX"),
        NFO("NFO"),
        NCO("NCO"),
        UNKNOWN("UNKNOWN");
        
        private final String value;
        
        private static final Map<String, Exchange> lookup = new HashMap<>();
        
        static {
            for (Exchange exchange : Exchange.values()) {
                lookup.put(exchange.getValue(), exchange);
            }
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        public static Exchange fromValue(String value) {
            return lookup.getOrDefault(value, UNKNOWN);
        }
    }
}
