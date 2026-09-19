package com.am.marketdata.common.model.marketinfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OiData {
    @JsonProperty("total_puts")
    private Long totalPuts;
    @JsonProperty("total_calls")
    private Long totalCalls;
    @JsonProperty("spot_closing_price")
    private Double spotClosingPrice;
    private String expiry;
    @JsonProperty("call_put_oi_data_list")
    private List<OiStrikeRow> callPutOiDataList;
}
