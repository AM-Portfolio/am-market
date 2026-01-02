package com.am.marketdata.api.util;

import com.am.common.investment.model.historical.OHLCVTPoint;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for filtering historical data based on various strategies.
 */
public class HistoricalDataFilterUtil {

    /**
     * Filter parameters extracted from request
     */
    @Getter
    @AllArgsConstructor
    public static class FilterParams {
        private final String filterType;
        private final int filterFrequency;
        private final boolean isFiltered;
    }

    /**
     * Extract filter parameters from the request params map
     * 
     * @param params Request parameters
     * @return FilterParams object with extracted values
     */
    public static FilterParams extractFilterParams(Map<String, Object> params) {
        if (params == null) {
            return new FilterParams("ALL", 1, false);
        }

        String filterType = params.containsKey("filterType") ? params.get("filterType").toString() : "ALL";

        int filterFrequency = params.containsKey("filterFrequency")
                ? Integer.parseInt(params.get("filterFrequency").toString())
                : 1;

        if ("CUSTOM".equalsIgnoreCase(filterType) && filterFrequency < 2) {
            filterFrequency = 2;
        }

        boolean isFiltered = !"ALL".equalsIgnoreCase(filterType);

        return new FilterParams(filterType, filterFrequency, isFiltered);
    }

    /**
     * Apply filtering strategy to data points based on filter type
     * 
     * @param dataPoints   Original list of OHLCVT data points
     * @param filterParams Filter parameters
     * @return Filtered list of data points
     */
    public static List<OHLCVTPoint> applyFilterStrategy(List<OHLCVTPoint> dataPoints, FilterParams filterParams) {
        List<OHLCVTPoint> filteredPoints = new ArrayList<>();

        if ("START_END".equalsIgnoreCase(filterParams.getFilterType())) {
            filteredPoints.add(dataPoints.get(0));

            if (dataPoints.size() > 1) {
                filteredPoints.add(dataPoints.get(dataPoints.size() - 1));
            }
        } else if ("CUSTOM".equalsIgnoreCase(filterParams.getFilterType())) {
            for (int i = 0; i < dataPoints.size(); i += filterParams.getFilterFrequency()) {
                filteredPoints.add(dataPoints.get(i));
            }

            int lastIndex = dataPoints.size() - 1;
            if (lastIndex >= 0 && lastIndex % filterParams.getFilterFrequency() != 0) {
                filteredPoints.add(dataPoints.get(lastIndex));
            }
        } else {
            return new ArrayList<>(dataPoints);
        }

        return filteredPoints;
    }
}
