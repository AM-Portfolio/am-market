package com.am.common.investment.service.mapper;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.persistence.influx.measurement.EquityPriceMeasurement;
import org.springframework.stereotype.Component;

@Component
public class EquityPriceMapper {
    
    public EquityPrice toModel(EquityPriceMeasurement measurement) {
        if (measurement == null) {
            return null;
        }
        
        EquityPrice model = new EquityPrice();
        model.setSymbol(measurement.getSymbol());
        model.setIsin(measurement.getIsin());
        model.setTime(measurement.getTime());
        model.setLastPrice(measurement.getLast());
        model.setOhlcv(OHLCVTPoint.builder()
        .open(measurement.getOpen())
        .close(measurement.getClose())
        .low(measurement.getLow())
        .high(measurement.getHigh())
        .volume(measurement.getVolume())
        .build());
        model.setExchange(measurement.getExchange());
        model.setCurrency(measurement.getCurrency());
        return model;
    }

    public EquityPriceMeasurement toMeasurement(EquityPrice model) {
        if (model == null) {
            return null;
        }
        
        EquityPriceMeasurement measurement = new EquityPriceMeasurement();
        measurement.setSymbol(model.getSymbol());
        measurement.setIsin(model.getIsin());
        measurement.setTime(model.getTime());
        measurement.setLast(model.getLastPrice());
        measurement.setOpen(model.getOhlcv().getOpen());
        measurement.setHigh(model.getOhlcv().getHigh());
        measurement.setLow(model.getOhlcv().getLow());
        measurement.setClose(model.getOhlcv().getClose());
        measurement.setVolume(model.getOhlcv().getVolume());
        measurement.setExchange(model.getExchange());
        measurement.setCurrency(model.getCurrency());
        return measurement;
    }
}
