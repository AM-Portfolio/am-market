package com.am.common.investment.model.historical.mapper;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.historical.OHLCVTPoint;

import java.util.List;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Mapper class for converting between EquityPrice and OHLCVTPoint models.
 * Provides bidirectional mapping capabilities.
 */
public class OHLCVTMapper {

    /**
     * Converts an EquityPrice to an OHLCVTPoint.
     * 
     * @param equityPrice the equity price to convert
     * @return the resulting OHLCVTPoint
     */
    public static OHLCVTPoint toOHLCVTPoint(EquityPrice equityPrice) {
        if (equityPrice == null) {
            return null;
        }
        
        return OHLCVTPoint.builder()
                .time(equityPrice.getTime().atZone(ZoneOffset.UTC).toLocalDateTime())
                .open(equityPrice.getOhlcv().getOpen())
                .high(equityPrice.getOhlcv().getHigh())
                .low(equityPrice.getOhlcv().getLow())
                .close(equityPrice.getOhlcv().getClose())
                .volume(equityPrice.getOhlcv().getVolume())
                .build();
    }
    
    /**
     * Converts an OHLCVTPoint to an EquityPrice.
     * Note: This conversion will not set symbol, isin, exchange, and currency fields.
     * These fields need to be set separately if needed.
     * 
     * @param ohlcvtPoint the OHLCVT point to convert
     * @return the resulting EquityPrice with OHLCVT data
     */
    public static EquityPrice toEquityPrice(OHLCVTPoint ohlcvtPoint) {
        if (ohlcvtPoint == null) {
            return null;
        }
        
        return EquityPrice.builder()
                .time(ohlcvtPoint.getTime().atZone(ZoneOffset.UTC).toInstant())
                .ohlcv(ohlcvtPoint)
                .build();
    }
    
    /**
     * Converts an OHLCVTPoint to an EquityPrice with additional metadata.
     * 
     * @param ohlcvtPoint the OHLCVT point to convert
     * @param symbol the symbol to set
     * @param isin the ISIN to set
     * @param exchange the exchange to set
     * @param currency the currency to set
     * @return the resulting EquityPrice with complete data
     */
    public static EquityPrice toEquityPrice(OHLCVTPoint ohlcvtPoint, String symbol, String isin, 
                                           String exchange, String currency) {
        if (ohlcvtPoint == null) {
            return null;
        }
        
        return EquityPrice.builder()
                .symbol(symbol)
                .isin(isin)
                .time(ohlcvtPoint.getTime().atZone(ZoneOffset.UTC).toInstant())
                .ohlcv(ohlcvtPoint)
                .exchange(exchange)
                .currency(currency)
                .build();
    }
    
    /**
     * Converts a list of EquityPrice objects to a list of OHLCVTPoint objects.
     * 
     * @param equityPrices the list of equity prices to convert
     * @return the resulting list of OHLCVTPoint objects
     */
    public static List<OHLCVTPoint> toOHLCVTPoints(List<EquityPrice> equityPrices) {
        if (equityPrices == null) {
            return new ArrayList<>();
        }
        
        return equityPrices.stream()
                .map(OHLCVTMapper::toOHLCVTPoint)
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a list of OHLCVTPoint objects to a list of EquityPrice objects.
     * Note: This conversion will not set symbol, isin, exchange, and currency fields.
     * 
     * @param ohlcvtPoints the list of OHLCVT points to convert
     * @return the resulting list of EquityPrice objects
     */
    public static List<EquityPrice> toEquityPrices(List<OHLCVTPoint> ohlcvtPoints) {
        if (ohlcvtPoints == null) {
            return new ArrayList<>();
        }
        
        return ohlcvtPoints.stream()
                .map(OHLCVTMapper::toEquityPrice)
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a list of OHLCVTPoint objects to a list of EquityPrice objects with additional metadata.
     * 
     * @param ohlcvtPoints the list of OHLCVT points to convert
     * @param symbol the symbol to set for all equity prices
     * @param isin the ISIN to set for all equity prices
     * @param exchange the exchange to set for all equity prices
     * @param currency the currency to set for all equity prices
     * @return the resulting list of EquityPrice objects with complete data
     */
    public static List<EquityPrice> toEquityPrices(List<OHLCVTPoint> ohlcvtPoints, String symbol, String isin, 
                                                  String exchange, String currency) {
        if (ohlcvtPoints == null) {
            return new ArrayList<>();
        }
        
        return ohlcvtPoints.stream()
                .map(point -> toEquityPrice(point, symbol, isin, exchange, currency))
                .collect(Collectors.toList());
    }
}
