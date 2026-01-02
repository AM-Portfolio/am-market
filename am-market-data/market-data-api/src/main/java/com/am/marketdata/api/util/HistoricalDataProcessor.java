package com.am.marketdata.api.util;

import com.am.common.investment.model.historical.HistoricalData;
import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.log.AppLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processor for historical data extraction and transformation.
 */
@Component
public class HistoricalDataProcessor {

    private final AppLogger log = AppLogger.getLogger();

    /**
     * Container for extracted historical data information
     */
    @Getter
    @AllArgsConstructor
    public static class HistoricalDataInfo {
        private final List<OHLCVTPoint> dataPoints;
        private final String tradingSymbol;
        private final String interval;
    }

    /**
     * Extract historical data information from a response map
     * 
     * @param data Response data map containing historical data
     * @return HistoricalDataInfo object or null if extraction fails
     */
    @SuppressWarnings("unchecked")
    public HistoricalDataInfo extractHistoricalDataInfo(Map<String, Object> data) {
        Object dataObj = data.get("data");
        List<OHLCVTPoint> dataPoints = new ArrayList<>();
        String tradingSymbol = "";
        String interval = "";

        if (dataObj instanceof HistoricalData) {
            HistoricalData historicalData = (HistoricalData) dataObj;
            dataPoints = historicalData.getDataPoints();
            tradingSymbol = historicalData.getTradingSymbol();
            interval = historicalData.getInterval();
        } else if (dataObj instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) dataObj;

            if (dataMap.containsKey("tradingSymbol")) {
                tradingSymbol = dataMap.get("tradingSymbol").toString();
            }

            if (dataMap.containsKey("interval")) {
                interval = dataMap.get("interval").toString();
            }

            if (dataMap.containsKey("dataPoints") && dataMap.get("dataPoints") instanceof List) {
                List<Map<String, Object>> pointMaps = (List<Map<String, Object>>) dataMap.get("dataPoints");

                for (Map<String, Object> pointMap : pointMaps) {
                    OHLCVTPoint point = new OHLCVTPoint();

                    if (pointMap.containsKey("timestamp")) {
                        if (pointMap.get("timestamp") instanceof Date) {
                            Date date = (Date) pointMap.get("timestamp");
                            point.setTime(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                        } else if (pointMap.get("timestamp") instanceof LocalDateTime) {
                            point.setTime((LocalDateTime) pointMap.get("timestamp"));
                        }
                    }

                    if (pointMap.containsKey("open"))
                        point.setOpen(Double.parseDouble(pointMap.get("open").toString()));
                    if (pointMap.containsKey("high"))
                        point.setHigh(Double.parseDouble(pointMap.get("high").toString()));
                    if (pointMap.containsKey("low"))
                        point.setLow(Double.parseDouble(pointMap.get("low").toString()));
                    if (pointMap.containsKey("close"))
                        point.setClose(Double.parseDouble(pointMap.get("close").toString()));
                    if (pointMap.containsKey("volume"))
                        point.setVolume(Long.parseLong(pointMap.get("volume").toString()));

                    dataPoints.add(point);
                }
            }
        } else {
            log.warn("extractHistoricalDataInfo",
                    "Unexpected data type for filtering: " + (dataObj != null ? dataObj.getClass().getName() : "null"));
            return null;
        }

        if (dataPoints.isEmpty()) {
            log.warn("extractHistoricalDataInfo", "No data points found for filtering");
            return null;
        }

        return new HistoricalDataInfo(dataPoints, tradingSymbol, interval);
    }

    /**
     * Apply data filtering to a response map
     * 
     * @param data   Response data map
     * @param params Filter parameters
     * @return Filtered response map
     */
    public Map<String, Object> applyDataFiltering(Map<String, Object> data, Map<String, Object> params) {
        HistoricalDataFilterUtil.FilterParams filterParams = HistoricalDataFilterUtil.extractFilterParams(params);

        if (!filterParams.isFiltered()) {
            return data;
        }

        Map<String, Object> result = new HashMap<>(data);
        HistoricalDataInfo dataInfo = extractHistoricalDataInfo(data);

        if (dataInfo == null || dataInfo.getDataPoints().isEmpty()) {
            return data;
        }

        List<OHLCVTPoint> originalPoints = dataInfo.getDataPoints();
        List<OHLCVTPoint> filteredPoints = HistoricalDataFilterUtil.applyFilterStrategy(originalPoints, filterParams);

        HistoricalData filteredData = new HistoricalData();
        filteredData.setDataPoints(filteredPoints);
        filteredData.setTradingSymbol(dataInfo.getTradingSymbol());
        filteredData.setInterval(dataInfo.getInterval());

        result.put("data", filteredData);
        result.put("count", filteredPoints.size());
        result.put("filtered", true);
        result.put("filterType", filterParams.getFilterType());
        result.put("originalCount", originalPoints.size());

        return result;
    }
}
