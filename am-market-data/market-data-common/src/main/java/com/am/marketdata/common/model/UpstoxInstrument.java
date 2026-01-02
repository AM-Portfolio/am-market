package com.am.marketdata.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "upstock_instruments")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxInstrument {

    @Id
    @JsonProperty("instrument_key")
    private String instrumentKey;

    @JsonProperty("isin")
    private String isin;

    @Indexed
    @JsonProperty("name")
    private String name;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("expiry")
    private Long expiry;

    @JsonProperty("instrument_type")
    @Field("instrument_type")
    private String instrumentType;

    @Indexed
    @JsonProperty("asset_symbol")
    @Field("asset_symbol")
    private String assetSymbol;

    @JsonProperty("underlying_key")
    @Field("underlying_key")
    private String underlyingKey;

    @JsonProperty("underlying_type")
    @Field("underlying_type")
    private String underlyingType;

    @JsonProperty("underlying_symbol")
    @Field("underlying_symbol")
    private String underlyingSymbol;

    @JsonProperty("lot_size")
    @Field("lot_size")
    private Double lotSize;

    @JsonProperty("tick_size")
    @Field("tick_size")
    private Double tickSize;

    @JsonProperty("freeze_quantity")
    @Field("freeze_quantity")
    private Double freezeQuantity;

    @JsonProperty("trading_symbol")
    @Field("trading_symbol")
    private String tradingSymbol;

    @JsonProperty("exchange_token")
    @Field("exchange_token")
    private String exchangeToken;

    @JsonProperty("minimum_lot")
    @Field("minimum_lot")
    private Double minimumLot;

    @JsonProperty("asset_type")
    @Field("asset_type")
    private String assetType;

    @JsonProperty("strike_price")
    @Field("strike_price")
    private Double strikePrice;

    @JsonProperty("qty_multiplier")
    @Field("qty_multiplier")
    private Double qtyMultiplier;

    @JsonProperty("segment")
    @Field("segment")
    private String segment;

    @JsonProperty("weekly")
    @Field("weekly")
    private Boolean weekly;

}
