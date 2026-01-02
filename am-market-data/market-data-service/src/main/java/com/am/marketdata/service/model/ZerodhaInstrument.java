package com.am.marketdata.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "zerodha_instruments")
public class ZerodhaInstrument {

    @Id
    @JsonProperty("instrument_token")
    @Field("instrument_token")
    private String instrumentToken;

    @JsonProperty("exchange_token")
    @Field("exchange_token")
    private String exchangeToken;

    @Indexed
    @JsonProperty("tradingsymbol")
    @Field("tradingsymbol")
    private String tradingSymbol;

    @JsonProperty("name")
    private String name;

    @JsonProperty("last_price")
    @Field("last_price")
    private Double lastPrice;

    @JsonProperty("expiry")
    private String expiry;

    @JsonProperty("strike")
    private Double strike;

    @JsonProperty("tick_size")
    @Field("tick_size")
    private Double tickSize;

    @JsonProperty("lot_size")
    @Field("lot_size")
    private Integer lotSize;

    @JsonProperty("instrument_type")
    @Field("instrument_type")
    private String instrumentType;

    @JsonProperty("segment")
    @Field("segment")
    private String segment;

    @JsonProperty("exchange")
    private String exchange;

}
