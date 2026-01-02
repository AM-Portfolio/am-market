# Market Data Analysis - Python Backend Design

## 1. Overview
We are switching the backend implementation for Market Analysis to **Python** to leverage its rich ecosystem for data science and technical analysis. Specifically, we will use the **`pandas-ta`** library, which serves as a powerful "plugin" for calculating hundreds of technical indicators out of the box with minimal code.

## 2. Architecture Diagram

![Python Analysis Architecture](market_analysis_python_architecture.png)

*(If image is unavailable, refer to the diagram below)*

```mermaid
graph TD
    Client[Client (Web/Mobile)] -->|HTTP Request| API
    
    subgraph "Python Service (FastAPI)"
        API[FastAPI Router]
        Service[Analysis Service]
        Engine[Pandas-TA Engine]
        
        subgraph "Data Structures"
            DataFrame[Pandas DataFrame]
        end
    end
    
    subgraph "Data Source"
        DB[(Market Data DB)]
    end
    
    API --> Service
    Service -->|Fetch OHLCV| DB
    DB --> DataFrame
    Service -->|Apply Indicators| Engine
    Engine -->|Enrich| DataFrame
    DataFrame -->|JSON| API
```

## 3. Technology Stack
*   **Language**: Python 3.10+
*   **Framework**: **FastAPI** (High performance, easy documentation)
*   **Analysis Library**: **`pandas-ta`** (The "Plugin" for indicators)
*   **Data Manipulation**: **`pandas`**
*   **Server**: `Uvicorn`

## 4. Key Components

### A. The "Plugin": Pandas-TA
Instead of writing a complex `IndicatorFactory` manually, we leverage `pandas-ta`.
*   **Usage**: `df.ta.strategy("All")` or `df.ta.sma(length=50)`.
*   **Benefit**: Instant access to 130+ indicators (SMA, EMA, RSI, MACD, Bollinger Bands, Ichimoku, etc.).

### B. Custom Analysis Strategy
We can define "Strategies" in JSON and pass them to the engine.
*   **Strategy Example**:
    ```python
    MyStrategy = ta.Strategy(
        name="Momo",
        ta=[
            {"kind": "sma", "length": 50},
            {"kind": "sma", "length": 200},
            {"kind": "rsi", "length": 14}
        ]
    )
    df.ta.strategy(MyStrategy)
    ```

### C. API Endpoints (`main.py`)

**1. Calculate Indicators**
*   `POST /analyze`
*   Input: `{"symbol": "AAPL", "indicators": [{"kind": "sma", "length": 50}]}`
*   Output: JSON with calculated values.

**2. Screener**
*   `POST /screen`
*   Input: `{"universe": ["AAPL", "GOOG"], "criteria": "RSI_14 > 70"}`
*   Output: List of matching symbols.

## 5. Directory Structure
We will create a new service directory: `am-market/market-data-analysis-py`

```
market-data-analysis-py/
├── app/
│   ├── main.py            # Entry point
│   ├── api/
│   │   └── routes.py      # Endpoints
│   ├── core/
│   │   └── analysis.py    # Pandas-TA logic
│   └── models/
│       └── schemas.py     # Pydantic models
├── requirements.txt
└── Dockerfile
```

## 6. Implementation Plan
1.  **Initialize Project**: Create directory and `requirements.txt`.
2.  **Install Dependencies**: `pandas`, `pandas-ta`, `fastapi`, `uvicorn`.
3.  **Implement Engine**: wrapper around `df.ta`.
4.  **Dockerize**: Create Dockerfile to run the service.
5.  **Integrate**: Add to `docker-compose.yml` on port **8010** (or similar available port).
6.  **Update SDK**: Generate Dart Client from FastAPI OpenAPI spec.
