package com.am.marketdata.provider.upstox.mapper;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.mapper.ProviderResponseMapper;
import com.am.marketdata.common.model.CommonInstrument;
import com.am.marketdata.common.model.CommonQuote;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.ProviderMetadata;
import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.marketdata.provider.upstox.model.HistoricalDataResponse;
import com.am.marketdata.provider.upstox.model.OHLCResponse;
import com.upstox.api.MarketQuoteSymbolLtpV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Upstox response mapper
 * Extracted from inline mapping logic in UpstoxMarketDataProvider
 */
@Slf4j
@Component("upstoxResponseMapper")
public class UpstoxResponseMapper implements ProviderResponseMapper {

    @Override
    public CommonQuote mapQuote(Object vendorQuote) {
        // Upstox doesn't have a dedicated Quote object yet
        // This is a placeholder for future implementation
        log.warn("Quote mapping not implemented for Upstox");
        return null;
    }

    @Override
    public OHLCQuote mapOHLC(Object vendorOHLC) {
        if (!(vendorOHLC instanceof OHLCResponse.OHLCData)) {
            log.warn("Invalid vendor OHLC type for Upstox: {}", vendorOHLC.getClass());
            return null;
        }

        OHLCResponse.OHLCData data = (OHLCResponse.OHLCData) vendorOHLC;

        OHLCQuote quote = new OHLCQuote();
        quote.setLastPrice(data.getLast_price() != null ? data.getLast_price() : 0.0);

        if (data.getOhlc() != null) {
            OHLCQuote.OHLC ohlc = new OHLCQuote.OHLC();
            ohlc.setOpen(data.getOhlc().getOpen());
            ohlc.setHigh(data.getOhlc().getHigh());
            ohlc.setLow(data.getOhlc().getLow());
            ohlc.setClose(data.getOhlc().getClose());
            quote.setOhlc(ohlc);
        }

        if (data.getPrevious_close() != null) {
            quote.setPreviousClose(data.getPrevious_close());
        }

        return quote;
    }

    @Override
    public HistoricalData mapHistoricalData(Object vendorData) {
        if (!(vendorData instanceof HistoricalDataResponse)) {
            log.warn("Invalid vendor historical data type for Upstox: {}", vendorData.getClass());
            return new HistoricalData();
        }

        HistoricalDataResponse response = (HistoricalDataResponse) vendorData;
        HistoricalData historicalData = new HistoricalData();

        if (response.getData() == null || response.getData().getCandles() == null) {
            return historicalData;
        }

        List<OHLCVTPoint> dataPoints = new ArrayList<>();

        for (List<Object> rawCandle : response.getData().getCandles()) {
            if (rawCandle == null || rawCandle.size() < 5) {
                continue;
            }

            OHLCVTPoint point = new OHLCVTPoint();

            try {
                // Index 0: Timestamp (String)
                String timestamp = (String) rawCandle.get(0);
                if (timestamp != null) {
                    // Upstox format: "2024-04-12T00:00:00+05:30"
                    Instant instant = Instant.parse(timestamp.replace("+0530", "+05:30"));
                    point.setTime(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
                } else {
                    point.setTime(LocalDateTime.now());
                }
            } catch (Exception e) {
                log.warn("Error parsing candle timestamp: {}", e.getMessage());
                point.setTime(LocalDateTime.now());
            }

            try {
                // Parse OHLCV data
                point.setOpen(parseDouble(rawCandle.get(1)));
                point.setHigh(parseDouble(rawCandle.get(2)));
                point.setLow(parseDouble(rawCandle.get(3)));
                point.setClose(parseDouble(rawCandle.get(4)));

                if (rawCandle.size() > 5) {
                    point.setVolume(parseLong(rawCandle.get(5)));
                } else {
                    point.setVolume(0L);
                }
            } catch (Exception e) {
                log.warn("Error parsing candle data points: {}", e.getMessage());
                continue;
            }

            dataPoints.add(point);
        }

        historicalData.setDataPoints(dataPoints);
        return historicalData;
    }

    @Override
    public CommonInstrument mapInstrument(Object vendorInstrument) {
        if (!(vendorInstrument instanceof UpstoxInstrument)) {
            log.warn("Invalid vendor instrument type for Upstox: {}", vendorInstrument.getClass());
            return null;
        }

        UpstoxInstrument upstoxInstrument = (UpstoxInstrument) vendorInstrument;

        CommonInstrument commonInstrument = CommonInstrument.builder()
                .instrumentKey(upstoxInstrument.getTradingSymbol())
                .tradingSymbol(upstoxInstrument.getTradingSymbol())
                .name(upstoxInstrument.getName())
                .exchange(upstoxInstrument.getExchange())
                .segment(upstoxInstrument.getSegment())
                .instrumentType(upstoxInstrument.getInstrumentType())
                .lotSize(upstoxInstrument.getLotSize() != null ? upstoxInstrument.getLotSize().intValue() : null)
                .tickSize(upstoxInstrument.getTickSize())
                .isin(upstoxInstrument.getIsin())
                .providerName("upstox")
                .providerInstrumentKey(upstoxInstrument.getInstrumentKey())
                .build();

        // Add provider metadata
        ProviderMetadata metadata = ProviderMetadata.builder()
                .providerName("upstox")
                .instrumentKey(upstoxInstrument.getInstrumentKey())
                .exchange(upstoxInstrument.getExchange())
                .segment(upstoxInstrument.getSegment())
                .originalToken(upstoxInstrument.getInstrumentKey())
                .build();

        commonInstrument.getProviderMetadata().put("upstox", metadata);

        return commonInstrument;
    }

    @Override
    public CommonQuote mapLTP(Object vendorLTP) {
        if (!(vendorLTP instanceof MarketQuoteSymbolLtpV3)) {
            log.warn("Invalid vendor LTP type for Upstox: {}", vendorLTP.getClass());
            return null;
        }

        MarketQuoteSymbolLtpV3 data = (MarketQuoteSymbolLtpV3) vendorLTP;

        return CommonQuote.builder()
                .lastTradedPrice(data.getLastPrice())
                .providerName("upstox")
                .build();
    }

    @Override
    public Map<String, CommonQuote> mapQuotes(Map<String, ?> vendorQuotes) {
        if (vendorQuotes == null) {
            return new HashMap<>();
        }

        return vendorQuotes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> mapQuote(entry.getValue())));
    }

    @Override
    public Map<String, OHLCQuote> mapOHLCs(Map<String, ?> vendorOHLCs) {
        if (vendorOHLCs == null) {
            return new HashMap<>();
        }

        Map<String, OHLCQuote> result = new HashMap<>();

        for (Map.Entry<String, ?> entry : vendorOHLCs.entrySet()) {
            OHLCQuote mapped = mapOHLC(entry.getValue());
            if (mapped != null) {
                result.put(entry.getKey(), mapped);
            }
        }

        return result;
    }

    @Override
    public Map<String, CommonQuote> mapLTPs(Map<String, ?> vendorLTPs) {
        if (vendorLTPs == null) {
            return new HashMap<>();
        }

        return vendorLTPs.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> mapLTP(entry.getValue())));
    }

    @Override
    public String getProviderName() {
        return "upstox";
    }

    /**
     * Parse object to Double
     * Handles both Number and String types
     */
    private Double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        return 0.0;
    }

    /**
     * Parse object to Long
     * Handles both Number and String types
     */
    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return 0L;
    }
}
