package com.am.marketdata.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketIndex {
    private String indexName;
    private String indexSymbol;
    private BigDecimal previousClose;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal last;
    private BigDecimal change;
    private BigDecimal percentChange;
    private Long volume;
    private LocalDateTime timestamp;
    private String exchange;

    // Getters and Setters
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
    
    public String getIndexSymbol() { return indexSymbol; }
    public void setIndexSymbol(String indexSymbol) { this.indexSymbol = indexSymbol; }
    
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }
    
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    
    public BigDecimal getLast() { return last; }
    public void setLast(BigDecimal last) { this.last = last; }
    
    public BigDecimal getChange() { return change; }
    public void setChange(BigDecimal change) { this.change = change; }
    
    public BigDecimal getPercentChange() { return percentChange; }
    public void setPercentChange(BigDecimal percentChange) { this.percentChange = percentChange; }
    
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}
