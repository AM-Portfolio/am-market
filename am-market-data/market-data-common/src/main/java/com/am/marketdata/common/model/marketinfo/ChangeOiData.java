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
public class ChangeOiData {
    @JsonProperty("total_put_change_oi")
    private Long totalPutChangeOi;
    @JsonProperty("total_call_change_oi")
    private Long totalCallChangeOi;
    @JsonProperty("spot_closing_price")
    private Double spotClosingPrice;
    private String expiry;
    @JsonProperty("call_put_oi_data_list")
    private List<OiStrikeRow> callPutOiDataList;
}
