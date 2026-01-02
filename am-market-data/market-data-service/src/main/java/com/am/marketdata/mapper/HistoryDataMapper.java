package com.am.marketdata.mapper;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper class to convert between Zerodha HistoricalData and AM Common HistoricalData models
 */
@Slf4j
@Component
public class HistoryDataMapper {

    /**
     * Convert a Zerodha historical data to AM common historical data model
     *
     * @param zerodhaHistoricalData Zerodha historical data model
     * @return AM common historical data model
     */
    public HistoricalData toCommonHistoricalData(com.zerodhatech.models.HistoricalData zerodhaHistoricalData) {
        if (zerodhaHistoricalData == null) {
            return null;
        }

        try {
            HistoricalData historicalData = new HistoricalData();
            List<OHLCVTPoint> dataPoints = new ArrayList<>();
            
            for (com.zerodhatech.models.HistoricalData data : zerodhaHistoricalData.dataArrayList) {
                OHLCVTPoint dataPoint = new OHLCVTPoint();
                // Convert string timestamp to Instant
                try {
                    // Parse ISO-8601 format with timezone offset (e.g., 2024-11-25T14:05:00+0530)
                    String timestampStr = data.timeStamp; // Using correct field name 'timeStamp'
                    Instant instant = Instant.parse(timestampStr.replace("+0530", "+05:30"));
                    dataPoint.setTime(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
                } catch (Exception e) {
                    log.warn("Failed to parse timestamp: {}", data.timeStamp, e);
                    // Use current time as fallback
                    dataPoint.setTime(LocalDateTime.now());
                }
                dataPoint.setOpen(data.open);
                dataPoint.setHigh(data.high);
                dataPoint.setLow(data.low);
                dataPoint.setClose(data.close);
                dataPoint.setVolume(data.volume);

                
                dataPoints.add(dataPoint);
            }
            
            historicalData.setDataPoints(dataPoints);
            
            return historicalData;
        } catch (Exception e) {
            log.error("Error converting Zerodha historical data to common model: {}", e.getMessage(), e);
            return null;
        }
    }

}
