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

        @JsonProperty("last_price")
        private Double lastPrice;

        @JsonProperty("instrument_token")
        private String instrumentToken;

        @JsonProperty("previous_close")
        private Double previousClose;

        public Double getLastPrice() { return lastPrice; }
        public void setLastPrice(Double lastPrice) { this.lastPrice = lastPrice; }
        public String getInstrumentToken() { return instrumentToken; }
        public void setInstrumentToken(String instrumentToken) { this.instrumentToken = instrumentToken; }
        public Double getPreviousClose() { return previousClose; }
        public void setPreviousClose(Double previousClose) { this.previousClose = previousClose; }

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
            if (instrumentToken == null) {
                log.debug("instrumentToken is null");
                return null;
            }
            log.debug("Processing instrumentToken: {}", instrumentToken);
            // Extract ISIN from format like "NSE_EQ|INF204KB16I7"
            int pipeIndex = instrumentToken.indexOf('|');
            if (pipeIndex >= 0 && pipeIndex + 1 < instrumentToken.length()) {
                String isin = instrumentToken.substring(pipeIndex + 1);
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