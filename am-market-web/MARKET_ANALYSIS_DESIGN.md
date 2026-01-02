# Market Data Analysis Module - Design & Architecture

## 1. Overview
The **Market Data Analysis** module acts as an intelligent layer on top of raw market data. Its primary goal is to provide technical insights, calculate indicators (MA, RSI, etc.), and screen for trading opportunities (e.g., "Stocks crossing MA50") based on user-selected timeframes.

## 2. Architecture Diagram

```mermaid
graph TD
    User[User Interaction] --> UI_Layer
    
    subgraph "Presentation Layer (UI)"
        UI_Layer[Analysis Dashboard]
        IndicatorWidget[Indicator Selector]
        ScreenerPanel[Screener Configuration]
        ChartWidget[Interactive Technical Chart]
        ResultGrid[Stock Result Grid]
    end

    subgraph "State Management (Bloc/Cubit)"
        AnalysisCubit[Analysis Cubit]
        ScreenerBloc[Screener Bloc]
    end

    subgraph "Domain Layer (Business Logic)"
        IndicatorFactory[Indicator Factory] --> |Creates| Indicators
        StrategyEngine[Strategy Engine] --> |Uses| IndicatorFactory
        ScreenerLogic[Stock Screener Logic] --> |Apply| StrategyEngine
        
        subgraph "Indicators (Factory Pattern)"
            SMA[Simple Moving Average]
            EMA[Exponential Moving Average]
            RSI[Relative Strength Index]
            MACD[MACD]
        end
    end

    subgraph "Data Layer"
        AnalysisRepo[Analysis Repository]
        MarketDataSDK[Market Data SDK]
        CalcEngine[Calculation Engine (Dart/Backend)]
    end

    UI_Layer --> AnalysisCubit
    AnalysisCubit --> ScreenerLogic
    ScreenerLogic --> AnalysisRepo
    AnalysisRepo --> MarketDataSDK
```

## 3. Core Components

### A. Technical Indicator Engine (The "Factory")
A flexible factory pattern to generate indicator values dynamically.

*   **`TechnicalIndicator` (Abstract Base)**: Defines contract (`calculate(List<Candle> data)`).
*   **`IndicatorFactory`**: Singleton/Provider to instantiate indicators by type (`type: 'SMA', period: 50`).
*   **Implementations**:
    *   `MovingAverageIndicator`: Handles SMA, EMA, WMA.
    *   `MomentumIndicator`: Handles RSI, Stochastic.
    *   `TrendIndicator`: Handles MACD, Bollinger Bands.

### B. Screener Engine (The "Analysis")
Logic to filter the universe of stocks based on technical criteria.

*   **`ScreenerCriteria`**: Model defining rules (e.g., `Indicator(SMA, 50) < ClosePrice`).
*   **`AnalysisService`**: detailed analysis logic.
    *   *Input*: List of Stock Symbols, Timeframe (1D, 1W), Rules.
    *   *Process*: Fetch history → Calculate Indicators → Apply Rules → Return Matches.

### C. Visuals (The "Chart")
*   **`AnalysisChart`**: A specialized chart widget that can overlay calculated indicators on top of price action.

## 4. Implementation Plan

### Phase 1: Foundation & Factory
1.  **Create Module Structure**: `lib/features/market_analysis/`
2.  **Define Entities**: `OHLCV`, `IndicatorConfig`, `AnalysisResult`.
3.  **Implement `IndicatorFactory`**:
    *   Create algorithms for SMA, EMA (to start).
    *   Unit test the calculations.

### Phase 2: Analysis Logic
1.  **`AnalysisRepository`**: Connect to `MarketDataSdkService` to get historical data (`getHistoricalData`).
2.  **`ScreenerService`**: Implement the loop to process multiple stocks (Note: limit batch size to avoid rate limits).
3.  **State Management**: Create `AnalysisCubit`.

### Phase 3: User Interface
1.  **Dashboard Layout**: Split view (Screener Config Left, Results Right/Bottom).
2.  **Indicator Widget**: Dropdown to select "Moving Average 50", "RSI 14".
3.  **Result Visualization**: List of stocks satisfying the condition.

## 5. Directory Structure (Proposed)

```
lib/features/market_analysis/
├── domain/
│   ├── entities/
│   │   ├── indicator_config.dart
│   │   ├── analysis_criteria.dart
│   │   └── technical_indicator.dart
│   ├── factory/
│   │   ├── indicator_factory.dart
│   │   └── strategies/ (sma.dart, rsi.dart)
│   └── usecases/
│       └── run_screener_usecase.dart
├── data/
│   ├── repositories/
│   │   └── analysis_repository_impl.dart
│   └── datasources/
│       └── calculation_engine.dart
├── presentation/
│   ├── state/
│   │   └── analysis_cubit.dart
│   └── widgets/
│       ├── indicator_selector.dart
│       ├── screener_panel.dart
│       └── analysis_chart.dart
└── market_analysis_module.dart
```

## 6. Technical Considerations
*   **Performance**: Calculating indicators for 500+ stocks in Dart might be slow on the UI thread. Use `compute()` isolate for calculation heavy-lifting.
*   **Data Fetching**: We need historical candles (OHLC) to calculate indicators. The `MarketDataSDS` must support fetching batch historical data efficiently.
