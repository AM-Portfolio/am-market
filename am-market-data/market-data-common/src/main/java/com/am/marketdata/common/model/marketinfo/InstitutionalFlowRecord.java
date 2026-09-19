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
public class InstitutionalFlowRecord {
    @JsonProperty("time_stamp")
    private Long timeStamp;
    @JsonProperty("buy_amount")
    private Double buyAmount;
    @JsonProperty("sell_amount")
    private Double sellAmount;
    @JsonProperty("buy_contracts")
    private Long buyContracts;
    @JsonProperty("sell_contracts")
    private Long sellContracts;
    @JsonProperty("oi_contracts")
    private Long oiContracts;
    @JsonProperty("oi_amount")
    private Double oiAmount;
    @JsonProperty("total_long_contracts")
    private Long totalLongContracts;
    @JsonProperty("total_short_contracts")
    private Long totalShortContracts;
    @JsonProperty("total_call_long_contracts")
    private Long totalCallLongContracts;
    @JsonProperty("total_put_long_contracts")
    private Long totalPutLongContracts;
    @JsonProperty("total_call_short_contracts")
    private Long totalCallShortContracts;
    @JsonProperty("total_put_short_contracts")
    private Long totalPutShortContracts;
}
