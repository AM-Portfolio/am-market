# AM Market

This repository contains the market data management ecosystem for the Asset Management (AM) platform. It orchestrates three key services responsible for fetching, processing, parsing, and visualizing financial market data.

## 🏗️ Architecture Overview

The repository is structured into three main microservices:

| Service | Directory | Description | Port |
|---------|-----------|-------------|------|
| **Market Data Service** | `am-market-data` | **Core Backend**. Spring Boot service that fetches live/historical data from external APIs (Upstox, Zerodha), processes it, and publishes to Kafka. | `8092` |
| **Market Parser Service** | `am-parser` | **Data Processing**. Python (FastAPI) service for parsing complex financial documents (e.g., mutual fund statements) and storing structured data in MongoDB. | `9000` |
| **Market Data Web** | `am-market-web` | **Frontend**. Flutter web application for visualizing market trends, live ticker data, and managing data ingestion jobs. | `9002` |

---

## 🚀 Getting Started

### Prerequisites
*   Docker & Docker Compose
*   Access to `am-infra` (Global Infrastructure must be running: MongoDB, Postgres, Redis, Kafka)

### Installation & Deployment

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd am-market
    ```

2.  **Configure Environment:**
    The root `.env` file contains all critical configuration (Database connections, API keys, JWT secrets).
    *   Ensure `host.docker.internal` is accessible for connecting to global infra.

3.  **Deploy Services:**
    Run the global Docker Compose to start all services:
    ```bash
    docker-compose --env-file .env up -d --build
    ```

4.  **Access Applications:**
    *   **Web Dashboard:** [http://localhost:9002](http://localhost:9002)
    *   **Market Data API:** [http://localhost:8092/actuator/health](http://localhost:8092/actuator/health)
    *   **Parser API Docs:** [http://localhost:9000/docs](http://localhost:9000/docs)

---

## 🛠️ Local Development

For faster iteration during development, you can run services locally using **Poetry** scripts defined in `pyproject.toml`.

### 📦 Build Commands

| Command | Description | Example |
| :--- | :--- | :--- |
| `poetry run build` | Builds **all submodules** in `am-market-data`. | `poetry run build` |
| `poetry run build <alias>` | Builds **specific submodule** (e.g., `api`, `common`). | `poetry run build api` |
| `poetry run build data` | Builds the `am-common-investment-data` submodule. | `poetry run build data` |

> [!TIP]
> **Short Module Aliases Inside Build**: You can provide shorthand names (like `api`, `common`, `provider`, `scraper`) and it will automatically translate them into full Maven `-pl :market-data-<alias>` parameters!
> 
> *Example:* `poetry run build api common`

### 🚀 Run Commands

| Command | Description | Example |
| :--- | :--- | :--- |
| **`poetry run market`** | Builds and runs the **Market Data App** on port 8092. | `poetry run market` |
| `poetry run market --run` | **Skip Build**: Runs the pre-built `.jar` instantly (Instant Start). | `poetry run market --run` |
| `poetry run parser` | Runs the **Parser API** on port 8022. | `poetry run parser` |
| `poetry run analysis` | Runs the **Analysis API** on port 8010. | `poetry run analysis` |
| `poetry run ui` | Runs the **Market UI** locally with Flutter. | `poetry run ui` |
| `poetry run all` | Starts **all services** concurrently. | `poetry run all` |

---

## 📂 Repository Structure

```
am-market/
├── .env                  # Global environment configuration
├── docker-compose.yml    # Orchestrates all three services
├── am-market-data/       # Java/Spring Boot Backend source code
├── am-parser/            # Python/FastAPI Parser source code
└── am-market-web/        # Flutter Web Frontend source code
```

## 🔌 Integration with AM-Infra

This repository relies on the global `am-infra` stack. It connects to:
*   **MongoDB**: Stores historical market data and parsed documents.
*   **PostgreSQL**: Manages user portfolios and relational data.
*   **Redis**: Caches live stock prices and tokens.
*   **Kafka**: Streams real-time updates (`am-stock-price-update`) to other microservices.
*   **InfluxDB**: Time-series storage for high-frequency market data.
