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
