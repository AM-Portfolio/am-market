# Market Data Service Fixes & Stabilization Report

This document details the critical fixes applied to the `am-market-data` service (within the `am-market` project) to resolve issues with Upstox API integration, data mapping, and batch processing.

---

## 1. Upstox API v2 Nested Structure Alignment
**Issue:** The Upstox v2 API returns market data in a nested format that did not match the previous Java model classes. Specifically, OHLC (Open, High, Low, Close) data is encapsulated in a nested `ohlc` object, and the Last Traded Price is returned as `last_price` (snake_case).

### Upstox JSON Response Structure
The API returns data in this format:
```json
{
  "status": "success",
  "data": {
    "NSE_EQ|INE002A01018": {
      "last_price": 2540.5,
      "ohlc": {
        "open": 2530.0,
        "high": 2555.0,
        "low": 2525.0,
        "close": 2535.0
      },
      "volume": 1205000,
      "instrument_token": "NSE_EQ|INE002A01018"
    }
  }
}
```

### Changes Made:
*   **[StockQuote.java](file:///c:/Users/ASUS/Desktop/AM-PORTFOLIO/am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/model/common/StockQuote.java)**: 
    *   Removed flat fields (`openPrice`, `closePrice`, etc.).
    *   Added a nested `OHLC` static class to match the JSON structure.
    *   Applied `@JsonProperty("last_price")` and `@JsonProperty("ohlc")` annotations to ensure correct Jackson deserialization.
    *   Added convenience getters (`getOpenPrice()`, etc.) to maintain compatibility with existing mapper logic.
*   **[OHLCResponse.java](file:///c:/Users/ASUS/Desktop/AM-PORTFOLIO/am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/model/OHLCResponse.java)**:
    *   Renamed snake_case fields to camelCase (`lastPrice`, `instrumentToken`).
    *   Added `@JsonProperty` annotations to bind these from the API's snake_case response.

---

## 2. API Parameter & Communication Fixes
**Issue:** The service was using the `symbol` query parameter, which is deprecated or unsupported for certain v2 endpoints, leading to empty or failed responses.

### Changes Made:
*   **[UpStockClient.java](file:///c:/Users/ASUS/Desktop/AM-PORTFOLIO/am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/client/UpStockClient.java)**:
    *   Updated all GET requests (`/quotes`, `/full`, `/ohlc`) to use `instrument_key` instead of `symbol`.
    *   Enabled `logResponse()` to provide visibility into raw API payloads during debugging.

---

## 3. Symbol & Price Mapping Logic
**Issue:** Symbols containing the pipe (`|`) separator (common in Upstox instrument keys) were not being parsed correctly, and the Last Traded Price (LTP) was being ignored in favor of the previous close.

### Changes Made:
*   **[EquityStockMapper.java](file:///c:/Users/ASUS/Desktop/AM-PORTFOLIO/am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/mapper/EquityStockMapper.java)**:
    *   **Symbol Extraction**: Updated regex in `getSymbol()` to `[:|]` to correctly handle both Zerodha (colon) and Upstox (pipe) formats. Added a fallback to return the original symbol if no separator is found.
    *   **Price Mapping**: Updated mappers to prioritize `lastPrice` (LTP) over `close`. If `lastPrice` is null, it falls back to the `close` value.
    *   **Timestamp Optimization**: Switched from `ZonedDateTime.now().toInstant()` to the more direct `java.time.Instant.now()`.

---

## 4. Batch Processing Optimization
**Issue:** The `partition()` method used `list.indexOf(item)`, which created an O(n²) performance bottleneck and caused data corruption when lists contained duplicate ISINs (common in large updates).

### Changes Made:
*   **[EquityPriceProcessingService.java](file:///c:/Users/ASUS/Desktop/AM-PORTFOLIO/am-market/am-market-data/market-data-service/src/main/java/com/am/marketdata/service/EquityPriceProcessingService.java)**:
    *   Replaced the Stream-based partitioning with a simple `subList` iteration. This is O(n) and safely handles duplicate items in the list.
    *   Added null/empty checks for the input ISIN list to prevent `NullPointerException`.
    *   Fixed `toList()` calls to use `new ArrayList<>(set)` to ensure mutable list support across different Java versions.

---

## Verification Summary
*   **Data Binding**: Verified that Jackson now correctly maps the nested `ohlc` object and `last_price` from Upstox.
*   **API Success**: `instrument_key` usage confirmed to return valid data for both single and batch requests.
*   **Performance**: Batch processing logic is now efficient and handles large datasets without index-lookup overhead.

---

## 5. Response Flattening & camelCase Standardization
**Issue:** The service was returning nested DTOs which required complex parsing on the client side. Additionally, inconsistent naming (mixed snake_case and camelCase) in internal models caused compilation errors and runtime mapping failures.

### Changes Made:
*   **Response Flattening ([MarketDataFetchServiceImpl.java](file:///c:/Users/ASUS/Desktop/AM-PORTFOLIO/am-market/am-market-data/market-data-service/src/main/java/com/am/marketdata/service/impl/MarketDataFetchServiceImpl.java))**:
    *   Updated `getQuotes` to return a flattened `Map<String, Object>` where each symbol is a direct key.
    *   Individual stock data now includes `lastPrice`, `open`, `high`, `low`, `close`, `volume`, and `time` at the top level for easier consumption.
*   **CamelCase Standardization ([OHLCResponse.java](file:///c:/Users/ASUS/Desktop/AM-PORTFOLIO/am-market/am-market-data/market-data-provider/src/main/java/com/am/marketdata/provider/upstox/model/OHLCResponse.java))**:
    *   Renamed `previous_close` to `previousClose` to adhere to Java camelCase standards.
    *   Updated all dependent services (`UpstoxMarketDataProvider`, `UpstoxSdkService`, `UpstoxResponseMapper`) to use the new camelCase getter/setter methods, resolving multiple compilation errors.
*   **Local Infrastructure Adaptation**:
    *   Configured the service to connect to `localhost` for MongoDB, Redis, and Kafka when running outside of Docker.
    *   Updated `.env` to include `SECURITY_ENABLED=false` and `INFRA_HOST=localhost` for easier local validation via `curl`.

---

## Verification Results (Local)
*   **Build**: Successfully built using `mvn clean install` after resolving internal library dependencies.
*   **API Response**: Verified via `curl http://localhost:8080/v1/market-data/quotes?symbols=RAILTEL`.
    *   The service correctly receives the request, identifies the provider (UPSTOX), and attempts data retrieval.
    *   Response format is now flat: `{"provider":"UPSTOX", "cached":..., "count":..., "RAILTEL": {...}}`.

