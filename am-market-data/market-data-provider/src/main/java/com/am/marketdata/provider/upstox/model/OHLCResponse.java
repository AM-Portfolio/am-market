package com.am.marketdata.provider.upstox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Data
@Slf4j
public class OHLCResponse {
    private String status;
    private Map<String, OHLCData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OHLCData {
        @JsonProperty("ohlc")
        private OHLC ohlc;

        private Double last_price;

        private String instrument_token;

        private Double previous_close;

        public Double getOpen() {
            return ohlc != null ? ohlc.getOpen() : null;
        }

        public Double getHigh() {
            return ohlc != null ? ohlc.getHigh() : null;
        }

        public Double getLow() {
            return ohlc != null ? ohlc.getLow() : null;
        }

        public Double getClose() {
            return ohlc != null ? ohlc.getClose() : null;
        }

        public String getISIN() {
            if (instrument_token == null) {
                log.debug("instrumentToken is null");
                return null;
            }
            log.debug("Processing instrumentToken: {}", instrument_token);
            // Extract ISIN from format like "NSE_EQ|INF204KB16I7"
            int pipeIndex = instrument_token.indexOf('|');
            if (pipeIndex >= 0 && pipeIndex + 1 < instrument_token.length()) {
                String isin = instrument_token.substring(pipeIndex + 1);
                log.debug("Extracted ISIN: {}", isin);
                return isin;
            }
            log.debug("Could not extract ISIN from instrumentToken");
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OHLC {
        private Double open;
        private Double high;
        private Double low;
        private Double close;
    }
}