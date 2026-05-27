# Documentation of Market Analysis Timeframe Support

This document outlines the recent updates to support extended timeframes in the market analysis service.

## 1. Problem Statement
The user dashboard was experiencing failures when selecting specific timeframes:
- **Issue**: The `movers` endpoint failed for **6M** and **5Y** timeframes, resulting in server errors.
- **Root Cause**: These values were missing from the `TimeFrame` enum, and the backend lacked the logic to calculate reference dates (e.g., "closing price from 6 months ago") for these periods.
- **Impact**: Users could not view gainers/losers or accurate performance data for periods longer than 1 year.

## 2. Suggestive Changes & Implementation

### A. Core TimeFrame Support
We expanded the central `TimeFrame` enum to include the missing periods and configured them to use `DAY` as the base unit for data fetching.

**File**: `market-data-common/.../TimeFrame.java`
```java
// Added new enum values
THREE_MONTH("3M", "day", "day", true, 90),
SIX_MONTH("6M", "day", "day", true, 180),
FIVE_YEAR("5Y", "week", "week", true, 1825);

// Updated base timeframe logic
case THREE_MONTH:
case SIX_MONTH:
case FIVE_YEAR:
    return DAY;
```

### B. Reference Date Calculation
Updated the service logic to correctly subtract months/years from the current date to find the historical "previous close" point.

**File**: `market-data-service/.../SmartStockService.java`
```java
private LocalDate getStartDateForTimeFrame(LocalDate current, TimeFrame timeFrame) {
    switch (timeFrame) {
        case THREE_MONTH:
            return current.minusMonths(3);
        case SIX_MONTH:
            return current.minusMonths(6);
        case FIVE_YEAR:
            return current.minusYears(5);
        // ... existing cases
    }
}
```

### C. Historical Chart Mapping
Ensured the chart service recognizes `3M` and `6M` strings and maps them to appropriate data intervals.

**File**: `market-data-analysis/.../MarketAnalyticsService.java`
```java
case "3M":
    interval = "1D";
    from = to.minusMonths(3);
    break;
case "6M":
    interval = "1D";
    from = to.minusMonths(6);
    break;
```

### D. API Resilience
Added error handling to prevent server crashes if an unsupported timeframe is requested in the future.

**File**: `market-data-analysis/.../AnalysisController.java`
```java
try {
    tf = timeFrame != null ? TimeFrame.fromApiValue(timeFrame) : null;
} catch (IllegalArgumentException e) {
    log.error("getMovers", "Invalid timeframe requested: " + timeFrame);
    return ResponseEntity.badRequest().body(Map.of("error", "Invalid timeframe", "message", e.getMessage()));
}
```

## 3. Backward Compatibility
All changes are **backward compatible**:
- Existing parameters (`1D`, `1W`, `1M`, `1Y`) work exactly as before.
- Response structures remain unchanged.
- The change is strictly additive, expanding the capabilities of the current contract.
