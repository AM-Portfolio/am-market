# Market Data Analysis API - Access Guide

This guide explains how to access the Market Data Analysis Python service endpoints through different methods.

## Service Overview

- **Service**: Market Data Analysis (Python/FastAPI)
- **Container**: `market-data-analysis-service`
- **Internal Port**: 8010
- **Base Path**: `/v1`

## Access Methods

### 1. Live/Production (via Cloudflare Tunnel)

Access the service through the public domain with SSL:

```bash
# Base URL
https://am.munish.org/api/market/analysis

# Example: Analyze endpoint
curl -X POST https://am.munish.org/api/market/analysis/v1/analyze \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "symbol": "RELIANCE",
    "timeframe": "1D",
    "indicators": [
      {"kind": "SMA", "params": {"length": 20}},
      {"kind": "RSI", "params": {"length": 14}}
    ]
  }'

# Health check
curl https://am.munish.org/api/market/analysis/health
```

**How it works:**
1. Request hits Cloudflare → Traefik (port 8000)
2. Traefik matches rule: `PathPrefix('/api/market/analysis')`
3. Traefik strips `/api/market/analysis/` using `strip-api-market-analysis` middleware
4. Traefik forwards to `market-data-analysis-service:8010` with path `/v1/analyze`

---

### 2. Local Gateway (via Traefik)

Access through Traefik running locally on port 8000:

```bash
# Base URL
http://localhost:8000/api/market/analysis

# Example: Analyze endpoint
curl -X POST http://localhost:8000/api/market/analysis/v1/analyze \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "symbol": "RELIANCE",
    "timeframe": "1D",
    "indicators": [
      {"kind": "SMA", "params": {"length": 20}}
    ]
  }'

# Health check
curl http://localhost:8000/api/market/analysis/health
```

**How it works:**
1. Request hits local Traefik (port 8000)
2. Same routing rules as production
3. Forwards to Docker service `market-data-analysis-service:8010`

---

### 3. Direct/Localhost (Bypass Gateway)

Access the service directly on its exposed port:

```bash
# Base URL (direct to container port)
http://localhost:8010

# Example: Analyze endpoint
curl -X POST http://localhost:8010/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "RELIANCE",
    "timeframe": "1D",
    "indicators": [
      {"kind": "SMA", "params": {"length": 20}}
    ]
  }'

# Health check
curl http://localhost:8010/health
```

**Note:** When accessing directly, you must use the `/v1` prefix (not `/api/market/analysis/v1`) because you're bypassing Traefik's path rewriting.

---

## Endpoint Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/analyze` | POST | Perform technical analysis on a symbol |
| `/health` | GET | Service health check |

## Traefik Configuration

The routing is configured in `am-infra/traefik/dynamic.yaml`:

```yaml
# Router
market-analysis:
  rule: "PathPrefix(`/api/market/analysis`)"
  service: market-analysis
  priority: 1000
  middlewares:
    - strip-api-market-analysis
  entryPoints:
    - web

# Service
market-analysis:
  loadBalancer:
    servers:
      - url: "http://market-data-analysis-service:8010"

# Middleware
strip-api-market-analysis:
  replacePathRegex:
    regex: "^/api/market/analysis/(.*)"
    replacement: "/v1/$1"
```

## Port Mapping Reference

| Access Method | URL | Port | Path Rewriting |
|---------------|-----|------|----------------|
| **Live** | `https://am.munish.org/api/market/analysis/v1/analyze` | 443 → 8000 → 8010 | Yes (strips `/api/market/analysis/`) |
| **Gateway** | `http://localhost:8000/api/market/analysis/v1/analyze` | 8000 → 8010 | Yes (strips `/api/market/analysis/`) |
| **Direct** | `http://localhost:8010/v1/analyze` | 8010 | No |

## Testing Examples

### Using curl (Direct)
```bash
curl -X POST http://localhost:8010/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"symbol": "NIFTY", "timeframe": "1D", "indicators": [{"kind": "SMA", "params": {"length": 50}}]}'
```

### Using curl (via Gateway)
```bash
curl -X POST http://localhost:8000/api/market/analysis/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"symbol": "NIFTY", "timeframe": "1D", "indicators": [{"kind": "RSI", "params": {"length": 14}}]}'
```

### Using Postman
1. **URL**: `http://localhost:8000/api/market/analysis/v1/analyze`
2. **Method**: POST
3. **Headers**: `Content-Type: application/json`
4. **Body** (raw JSON):
```json
{
  "symbol": "RELIANCE",
  "timeframe": "1D",
  "indicators": [
    {"kind": "SMA", "params": {"length": 20}},
    {"kind": "EMA", "params": {"length": 50}},
    {"kind": "RSI", "params": {"length": 14}}
  ]
}
```

## Troubleshooting

### 404 Not Found
- **Via Gateway**: Ensure Traefik is running (`docker ps | grep traefik`)
- **Direct**: Ensure service is running (`docker ps | grep market-data-analysis`)
- Check the path includes `/v1` prefix

### Connection Refused
- Verify the service is running: `docker logs market-data-analysis-service`
- Check port mapping: `docker port market-data-analysis-service`

### 500 Internal Server Error
- Check service logs: `docker logs market-data-analysis-service --tail 50`
- Verify dependencies (MongoDB, Redis) are accessible
