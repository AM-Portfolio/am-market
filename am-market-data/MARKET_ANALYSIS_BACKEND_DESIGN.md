# Market Data Analysis - Backend Architecture & Design

## 1. Overview
This document outlines the architecture for the **Backend Market Analysis Module** within `am-market-data`. The goal is to provide a robust engine for calculating technical indicators (SMA, RSI, MACD, etc.) and screening stocks based on these indicators via a REST API.

## 2. Architecture Diagram

![Backend Analysis Architecture](market_data_backend_architecture.png)

*(If image is unavailable, refer to the diagram below)*

```mermaid
graph TD
    Client[Client (Web/Mobile)] -->|REST Request| Controller
    
    subgraph "am-market-data (Spring Boot)"
        subgraph "API Layer"
            Controller[AnalysisController]
        end
        
        subgraph "Service Layer"
            Service[AnalysisService]
            Factory[IndicatorFactory]
            StrategyEngine[StrategyEngine]
        end
        
        subgraph "Compute Layer"
            TAEngine[TechnicalAnalysisEngine (ta4j)]
        end
        
        subgraph "Data Layer"
            Repo[MarketDataRepository]
            Cache[Redis Cache]
        end
    end
    
    Controller --> Service
    Service --> Factory
    Service --> Repo
    Service --> TAEngine
    Repo -->|Fetch OHLCV| Database[(Postgres/Influx)]
    Factory -->|Create| TAEngine
```

## 3. Module Structure
A new Maven module `market-data-analysis` will be created to encapsulate this logic.

**Directory**: `am-market-data/market-data-analysis`

**Dependencies**:
*   `market-data-common`: For shared models (`Candle`, `Ticker`).
*   `ta4j-core`: For reliable technical analysis algorithms.
*   `spring-boot-starter-web`: For REST controllers.

## 4. Key Components

### A. Indicator Factory (`IndicatorFactory.java`)
Responsible for creating and configuring indicator instances based on request parameters.
*   *Input*: `IndicatorType` (Enum: SMA, RSI, EMA), `Period` (int), `Source` (Close, Open).
*   *Output*: `Indicator` object (ta4j interface).

### B. Analysis Service (`AnalysisService.java`)
Orchestrates the flow:
1.  Fetch historical data for the requested symbol/timeframe.
2.  Convert data to `BarSeries` (ta4j format).
3.  Invoke `IndicatorFactory` to create requested indicators.
4.  Run calculation on the `BarSeries`.
5.  Format result into `AnalysisResponse`.

### C. REST API (`AnalysisController.java`)

**Endpoint 1: Calculate Indicators**
*   **URL**: `POST /api/v1/analysis/indicators`
*   **Body**:
    ```json
    {
      "symbol": "AAPL",
      "timeframe": "1D",
      "indicators": [
         { "type": "SMA", "period": 50, "source": "CLOSE" },
         { "type": "RSI", "period": 14 }
      ]
    }
    ```
*   **Response**:
    ```json
    {
      "symbol": "AAPL",
      "latestValues": {
         "SMA_50": 150.23,
         "RSI_14": 65.4
      },
      "historicalValues": [...]
    }
    ```

**Endpoint 2: Screen Stocks**
*   **URL**: `POST /api/v1/analysis/screen`
*   **Body**: List of rules (e.g., `SMA_50 > ClosePrice`).
*   **Response**: List of matching symbols.

## 5. Implementation Plan

1.  **Setup Module**: Create `market-data-analysis` in `pom.xml`.
2.  **Add Dependencies**: Add `ta4j-core` to `pom.xml`.
3.  **Implement Factory**: Create `IndicatorFactory` with support for SMA, EMA, RSI.
4.  **Implement Service**: Create logic to convert `MarketData` to `BarSeries`.
5.  **Create Controller**: Expose the endpoints.
6.  **Update SDK**: Regenerate Flutter SDK to include new API.
