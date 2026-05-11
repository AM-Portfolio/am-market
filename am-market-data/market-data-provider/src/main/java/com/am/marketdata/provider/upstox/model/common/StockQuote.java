package com.am.marketdata.provider.upstox.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockQuote {

    private String symbol;
    private String isin;
    private String exchange;

    @JsonProperty("last_price")
    private Double lastPrice;

    @JsonProperty("ohlc")
    private OHLC ohlc;

    private Long volume;

    @JsonProperty("instrument_token")
    private String instrumentToken;

    @JsonProperty("previous_close")
    private Double previousClose;

    private Double change;

    @JsonProperty("change_percent")
    private Double changePercent;

    // Getters and Setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public Double getLastPrice() { return lastPrice; }
    public void setLastPrice(Double lastPrice) { this.lastPrice = lastPrice; }
    public OHLC getOhlc() { return ohlc; }
    public void setOhlc(OHLC ohlc) { this.ohlc = ohlc; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    public String getInstrumentToken() { return instrumentToken; }
    public void setInstrumentToken(String instrumentToken) { this.instrumentToken = instrumentToken; }
    public Double getPreviousClose() { return previousClose; }
    public void setPreviousClose(Double previousClose) { this.previousClose = previousClose; }
    public Double getChange() { return change; }
    public void setChange(Double change) { this.change = change; }
    public Double getChangePercent() { return changePercent; }
    public void setChangePercent(Double changePercent) { this.changePercent = changePercent; }

    // Convenience helpers so EquityStockMapper can still call getOpenPrice() etc.
    public Double getOpenPrice() { return ohlc != null ? ohlc.getOpen() : null; }
    public Double getHighPrice() { return ohlc != null ? ohlc.getHigh() : null; }
    public Double getLowPrice()  { return ohlc != null ? ohlc.getLow()  : null; }
    public Double getClosePrice(){ return ohlc != null ? ohlc.getClose(): null; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OHLC {
        private Double open;
        private Double high;
        private Double low;
        private Double close;

        public Double getOpen()  { return open;  }
        public void setOpen(Double open)   { this.open = open;   }
        public Double getHigh()  { return high;  }
        public void setHigh(Double high)   { this.high = high;   }
        public Double getLow()   { return low;   }
        public void setLow(Double low)     { this.low = low;     }
        public Double getClose() { return close; }
        public void setClose(Double close) { this.close = close; }
    }
}