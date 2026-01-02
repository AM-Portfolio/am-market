package com.am.marketdata.upstock.model;

import lombok.Data;
import java.util.List;

@Data
public class HistoricalDataResponse {
    private String status;
    private DataPayload data;

    @Data
    public static class DataPayload {
        private List<List<Object>> candles;
    }
}