package com.am.marketdata.common.model.marketinfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OiStrikeRow {
    @JsonProperty("call_oi")
    private Long callOi;
    @JsonProperty("put_oi")
    private Long putOi;
    @JsonProperty("strike_price")
    private Double strikePrice;
    @JsonProperty("call_change_oi")
    private Long callChangeOi;
    @JsonProperty("put_change_oi")
    private Long putChangeOi;
}
