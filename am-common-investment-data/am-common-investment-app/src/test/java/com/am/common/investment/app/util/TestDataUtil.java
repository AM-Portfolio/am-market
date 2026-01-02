package com.am.common.investment.app.util;

import com.am.common.investment.model.equity.EquityPrice;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.common.investment.model.stockindice.AuditData;
import com.am.common.investment.persistence.influx.measurement.EquityPriceMeasurement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.am.common.investment.model.board.BoardOfDirectors;

public class TestDataUtil {

    public static BoardOfDirectors readBoardOfDirectorsFromResource(String resourcePath) throws IOException {
        // Read the JSON content from the resource file
        String json = new String(Files.readAllBytes(Paths.get("src/test/resources/" + resourcePath)));
        // Use JsonUtils to parse the JSON into a BoardOfDirectors object
        return JsonUtils.fromJson(json, BoardOfDirectors.class);
    }

    public static AuditData createAudit(LocalDateTime updatedAt, String updatedBy) {
        AuditData audit = AuditData.builder()
        .createdAt(updatedAt)
        .updatedAt(updatedAt)
        .createdBy(updatedBy)
        .updatedBy(updatedBy)
        .build();
        return audit;
    }

    public static EquityPrice createEquityPrice(String symbol, String isin, Double open, Double high, Double low, 
            Double close, Long volume, String exchange, String currency, Instant time) {
        return EquityPrice.builder()
            .symbol(symbol)
            .isin(isin)
            .ohlcv(OHLCVTPoint.builder()
            .open(open)
            .high(high)
            .low(low)
            .close(close)
            .volume(volume)
            .build())
            .exchange(exchange)
            .currency(currency)
            .time(time)
            .build();
    }

    public static EquityPriceMeasurement createMeasurement(String symbol, String isin, Double open, Double high, Double low, 
            Double close, Long volume, String exchange, String currency, Instant time) {
        EquityPriceMeasurement measurement = new EquityPriceMeasurement();
        measurement.setSymbol(symbol);
        measurement.setIsin(isin);
        measurement.setOpen(open);
        measurement.setHigh(high);
        measurement.setLow(low);
        measurement.setClose(close);
        measurement.setVolume(volume);
        measurement.setExchange(exchange);
        measurement.setCurrency(currency);
        measurement.setTime(time);
        return measurement;
    }

    public static EquityPrice createSimpleEquityPrice(String symbol, Double close, Instant time) {
        return createEquityPrice(symbol, null, null, null, null, close, null, null, null, time);
    }

    public static EquityPriceMeasurement createSimpleMeasurement(String symbol, Double close, Instant time) {
        return createMeasurement(symbol, null, null, null, null, close, null, null, null, time);
    }
}
