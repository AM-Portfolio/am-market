package com.am.common.investment.persistence.influx.measurement;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Measurement(name = "equity")
public class EquityPriceMeasurement {
    @Column(tag = true, name = "symbol")
    private String symbol;
    
    @Column(tag = true, name = "isin")
    private String isin;
    
    @Column(timestamp = true, name = "time")
    private Instant time;

    @Column(tag = true, name = "exchange")
    private String exchange;

    @Column(name = "open")
    private Double open;
    
    @Column(name = "last")
    private Double last;
    
    @Column(name = "high")
    private Double high;
    
    @Column(name = "low")
    private Double low;
    
    @Column(name = "close")
    private Double close;
    
    @Column(name = "volume")
    private Long volume;
    
    @Column(name = "currency")
    private String currency;
}
