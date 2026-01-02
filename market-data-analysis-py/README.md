# Market Data Analysis Service (Python)

A FastAPI-based microservice for performing technical analysis on market data.

## Overview

This service provides REST API endpoints for analyzing market data using various technical indicators. It integrates with the Market Data backend to fetch historical data and performs analysis using pandas and technical analysis libraries.

## Architecture

```
market-data-analysis-py/
├── app/
│   ├── api/           # API routes
│   ├── core/          # Core configurations and auth
│   ├── models/        # Pydantic schemas
│   └── services/      # Business logic and external clients
├── docs/              # API documentation
├── v1/                # API collections
├── Dockerfile         # Container configuration
└── requirements.txt   # Python dependencies
```

## Features

- **Technical Analysis**: Support for various technical indicators
- **Historical Data Integration**: Fetches data from Market Data backend
- **REST API**: FastAPI-based endpoints
- **Authentication**: Integration with AM auth service
- **Docker Support**: Containerized deployment

## Technology Stack

- **Framework**: FastAPI
- **Data Processing**: Pandas
- **HTTP Client**: Requests
- **ASGI Server**: Uvicorn
- **Validation**: Pydantic v2

## API Endpoints

### POST /analyze
Analyzes market data for a given symbol with specified indicators.

**Request:**
```json
{
  "symbol": "NIFTY 50",
  "timeframe": "1d",
  "indicators": [
    {"kind": "sma", "params": {"period": 20}},
    {"kind": "rsi", "params": {"period": 14}}
  ]
}
```

**Response:**
```json
{
  "symbol": "NIFTY 50",
  "results": {
    "sma": [/* array of values */],
    "rsi": [/* array of values */]
  }
}
```

## Configuration

Environment variables (see `app/core/config.py`):
- `MARKET_DATA_BASE_URL`: Backend API URL
- `AUTH_SERVICE_URL`: Authentication service URL
- Port: `8010`

## Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Run the server
uvicorn app.main:app --reload --port 8010
```

## Docker Deployment

```bash
# Build
docker build -t market-data-analysis .

# Run
docker run -p 8010:8010 market-data-analysis
```

## Integration

This service is part of the AM Market ecosystem:
- Consumes: `am-market-data` (port 8020)
- Authenticates via: `am-auth` service
- Exposed on: Port `8010`

## Port Allocation

Following the global port strategy:
- **8100-8119**: AI & Analysis Services
- **This service**: `8010` (Analysis)
