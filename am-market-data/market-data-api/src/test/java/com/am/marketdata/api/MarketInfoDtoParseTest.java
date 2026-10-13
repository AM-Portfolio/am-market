package com.am.marketdata.api;

import com.am.marketdata.common.model.marketinfo.ChangeOiResponse;
import com.am.marketdata.common.model.marketinfo.InstitutionalFlowResponse;
import com.am.marketdata.common.model.marketinfo.OiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MarketInfoDtoParseTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesInstitutionalFlowPayload() throws Exception {
        String json = """
                {"status":"success","data":{"NSE_FO|INDEX_OPTIONS":[{
                  "time_stamp":1789669800000,"buy_amount":833775.01,"sell_amount":829370.24,
                  "buy_contracts":5451316,"sell_contracts":5419083,"oi_contracts":3618073,
                  "oi_amount":559755.87,"total_long_contracts":0,"total_short_contracts":0,
                  "total_call_long_contracts":690977,"total_put_long_contracts":1299085,
                  "total_call_short_contracts":959379,"total_put_short_contracts":668632
                }]}}
                """;

        InstitutionalFlowResponse response =
                objectMapper.readValue(json, InstitutionalFlowResponse.class);

        assertEquals("success", response.getStatus());
        assertEquals(833775.01,
                response.getData().get("NSE_FO|INDEX_OPTIONS").get(0).getBuyAmount());
        assertEquals(690977L,
                response.getData().get("NSE_FO|INDEX_OPTIONS").get(0).getTotalCallLongContracts());
    }

    @Test
    void parsesOiPayload() throws Exception {
        String json = """
                {"status":"success","data":{"total_puts":92818980,"total_calls":94165000,
                "spot_closing_price":23346.4,"expiry":"29-09-2026",
                "call_put_oi_data_list":[{"call_oi":910,"put_oi":814970,"strike_price":18000.0}]}}
                """;

        OiResponse response = objectMapper.readValue(json, OiResponse.class);

        assertEquals(92818980L, response.getData().getTotalPuts());
        assertEquals(23346.4, response.getData().getSpotClosingPrice());
        assertEquals(910L, response.getData().getCallPutOiDataList().get(0).getCallOi());
    }

    @Test
    void parsesChangeOiAndNullableData() throws Exception {
        String json = """
                {"status":"success","data":{"total_put_change_oi":7654960,
                "total_call_change_oi":5525845,"spot_closing_price":23346.4,
                "expiry":"29-09-2026","call_put_oi_data_list":[
                {"strike_price":15000.0,"call_change_oi":0,"put_change_oi":1690}]}}
                """;

        ChangeOiResponse response = objectMapper.readValue(json, ChangeOiResponse.class);
        OiResponse empty = objectMapper.readValue(
                "{\"status\":\"success\",\"data\":null}", OiResponse.class);

        assertEquals(7654960L, response.getData().getTotalPutChangeOi());
        assertEquals(1690L, response.getData().getCallPutOiDataList().get(0).getPutChangeOi());
        assertNotNull(empty);
    }
}
