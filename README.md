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
*   **Docker & Docker Compose**
*   **Access to `am-infra`** (Global Infrastructure must be running: MongoDB, Postgres, Redis, Kafka)
*   **`am-scripts` repository**: The scripts in this repo rely on `am-scripts` being available in the parent directory (`../am-scripts`).
*   **Base Images**: Some services build on top of base images. You **must** build these locally first to avoid `image not found` errors during builds or deployments:
    ```bash
    docker compose -f docker-compose.base.yml build
    # This prepares: am-java-maven-base, am-python-base, am-flutter-base
    ```

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
    *   **Parser API Docs:** [http://localhost:8022/docs](http://localhost:8022/docs) (Note: local port defaults to 8022)

---

## 🛠️ Local Development

For faster iteration during development, you can run services locally using **Poetry** scripts defined in `pyproject.toml`.

> [!IMPORTANT]
> **Prerequisite**: Run `poetry install` in the container/environment before running these scripts.

---

### 📦 Build & Run Commands

Available via `poetry run <command>`.

#### 1. `poetry run build` (Build Market Data)
*   **Usage**: `poetry run build [module_aliases...]`
*   **Default (No arguments)**: Builds **all submodules** in `am-market-data` (`clean install`).
*   **Arguments**:
    *   `[module_aliases]`: Shorthand names like `api`, `common`, `provider`, `scraper`.
    *   `data`: Builds `am-common-investment-data` (outside standard build).
*   **Work Done**: Identifies requested modules, verifies paths, and executes `mvn clean install` for the specific components.
*   **Example**: `poetry run build api common`

---

#### 2. `poetry run market` (Run Market Data Backend)
*   **Usage**: `poetry run market [port]`
*   **Default (No arguments)**: Runs on port **`8092`** using `mvn spring-boot:run`.
*   **Arguments**:
    *   `[port]`: Custom port number (e.g., `8093`).
*   **Flags**:
    *   `--run` or `--run-only`: **Skip Build** and run the pre-built `.jar` instantly (`java -jar`). Faster startup, requires a previous build.
*   **Work Done**: Loads environment variables from `.env` and boots the Java Spring Boot service.

---

#### 3. `poetry run parser` (Run Parser Service)
*   **Usage**: `poetry run parser`
*   **Default**: Runs on port **`8022`**.
*   **Work Done**: Starts the Python FastAPI application for parsing operations.

---

#### 4. `poetry run analysis` (Run Analysis Service)
*   **Usage**: `poetry run analysis`
*   **Default**: Runs on port **`8010`**.
*   **Work Done**: Starts the Python FastAPI application for market analysis.

---

#### 5. `poetry run ui` (Run Market UI)
*   **Usage**: `poetry run ui`
*   **Default**: Auto-detects Flutter device and runs on port **`9000`**.
*   **Work Done**: Launches the Flutter web application locally.

---

#### 6. `poetry run all` (Run All Services)
*   **Usage**: `poetry run all`
*   **Work Done**: Spins up the **Parser** API, **Analysis** API, and **Market Data** backend concurrently in a single terminal session.

---

### 🚀 Deployment Commands

#### `poetry run deploy-all` (Build & Deploy Local Stack)
*   **Usage**: `poetry run deploy-all [options]`
*   **Work Done**: Coordinates orchestration of local deployments. Builds service containers and manages loading/installing into local cluster contexts (e.g., KIND) or Docker execution triggers.
*   **If No Options Passed**: Builds and deployes all active configured services into standard cluster layers.
*   **Key Flags**:
    *   `--skip-build` / `-k`: Skip Docker builds (assumes image availability).
    *   `--build-only` / `-b`: Only trigger container builds without starting deployment cycles.
    *   `--deploy-only` / `-d`: Directly deploy using current artifacts, skipping build updates.
    *   `--services` / `-s`: Target specific names (e.g., `--services "am-market-data"`).
    *   `--namespace-prefix` / `-p`: Customize prefix layers (Default: `am`).
    *   `--run-docker`: Standard local container startup triggers instead of cluster templates.

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
