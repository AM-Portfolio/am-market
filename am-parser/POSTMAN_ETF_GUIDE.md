# ETF Holdings API - Postman Testing Guide

This guide shows how to test all ETF Holdings API endpoints using Postman.

## Server Setup
Make sure your server is running on: `http://127.0.0.1:8000`

## API Endpoints Overview

### Base URL
```
http://127.0.0.1:8000/etf
```

---

## 1. Get ETF Statistics

**Endpoint:** `GET /etf/stats`
**Purpose:** Get overview of ETF data and holdings coverage

### Postman Setup:
- **Method:** GET
- **URL:** `http://127.0.0.1:8000/etf/stats`
- **Headers:** None required

### Expected Response:
```json
{
  "etf_collection": {
    "total_etfs": 270,
    "etfs_with_isin": 270,
    "etfs_with_embedded_holdings": 4
  },
  "holdings_collection": {
    "total_holdings_records": 2,
    "collection_name": "etf_holdings"
  },
  "coverage": {
    "isin_coverage": "100.0%",
    "holdings_coverage": "0.7%"
  }
}
```

---

## 2. Search ETFs

**Endpoint:** `GET /etf/search`
**Purpose:** Find ETFs by symbol, name, or ISIN

### Postman Setup:
- **Method:** GET
- **URL:** `http://127.0.0.1:8000/etf/search`
- **Query Parameters:**
  - `query`: `nifty` (search term)
  - `limit`: `5` (max results)

### Full URL Example:
```
http://127.0.0.1:8000/etf/search?query=nifty&limit=5
```

### Expected Response:
```json
{
  "query": "nifty",
  "total_found": 5,
  "etfs": [
    {
      "symbol": "UTINIFTETF",
      "name": "UTI Nifty 50 ETF",
      "isin": "INF789F1AZC0",
      "asset_class": "Equity",
      "market_cap_category": "Large cap"
    }
  ]
}
```

---

## 3. Fetch Holdings for Single ETF

**Endpoint:** `POST /etf/fetch-holdings/{symbol}`
**Purpose:** Start background job to fetch holdings for specific ETF

### Postman Setup:
- **Method:** POST
- **URL:** `http://127.0.0.1:8000/etf/fetch-holdings/UTINIFTETF`
- **Headers:** 
  - `Content-Type: application/json`
- **Body:** Raw JSON (optional)
```json
{
  "callback_url": "https://webhook.site/your-unique-url",
  "user_id": "test_user_123"
}
```

### Expected Response:
```json
{
  "job_id": "job_abc123def456",
  "status": "pending",
  "message": "Started fetching holdings for ETF UTINIFTETF in background.",
  "estimated_completion_time": "2025-09-20 05:30:15",
  "status_url": "/jobs/job_abc123def456/status",
  "webhook_url": "https://webhook.site/your-unique-url"
}
```

---

## 4. Fetch All ETF Holdings

**Endpoint:** `POST /etf/fetch-all-holdings`
**Purpose:** Start background job to fetch holdings for all ETFs with ISINs

### Postman Setup:
- **Method:** POST
- **URL:** `http://127.0.0.1:8000/etf/fetch-all-holdings`
- **Query Parameters (optional):**
  - `limit`: `5` (limit number of ETFs to process)
  - `callback_url`: `https://webhook.site/your-unique-url`
  - `user_id`: `test_user_123`

### Full URL Example:
```
http://127.0.0.1:8000/etf/fetch-all-holdings?limit=5&callback_url=https://webhook.site/abc123&user_id=test_user
```

### Expected Response:
```json
{
  "job_id": "job_xyz789abc123",
  "status": "pending",
  "message": "Started fetching holdings for 5 ETFs in background.",
  "estimated_completion_time": "2025-09-20 05:32:00",
  "status_url": "/jobs/job_xyz789abc123/status",
  "webhook_url": "https://webhook.site/abc123"
}
```

---

## 5. Check Job Status

**Endpoint:** `GET /jobs/{job_id}/status`
**Purpose:** Monitor background job progress

### Postman Setup:
- **Method:** GET
- **URL:** `http://127.0.0.1:8000/jobs/{job_id}/status`
- Replace `{job_id}` with actual job ID from previous responses

### Example URL:
```
http://127.0.0.1:8000/jobs/job_abc123def456/status
```

### Expected Responses:

**While Running:**
```json
{
  "job_id": "job_abc123def456",
  "status": "running",
  "progress": {
    "total_items": 1,
    "completed_items": 0,
    "failed_items": 0,
    "current_item": "UTINIFTETF (INF789F1AZC0)",
    "percentage": 0.0
  },
  "result": null,
  "error_message": null,
  "created_at": "2025-09-20T05:30:10",
  "started_at": "2025-09-20T05:30:12",
  "completed_at": null,
  "estimated_remaining_time": "0.0 minutes"
}
```

**When Completed:**
```json
{
  "job_id": "job_abc123def456",
  "status": "completed",
  "progress": {
    "total_items": 1,
    "completed_items": 1,
    "failed_items": 0,
    "percentage": 100.0
  },
  "result": {
    "symbol": "UTINIFTETF",
    "isin": "INF789F1AZC0",
    "success": true,
    "operation": "fetch_single_holdings"
  },
  "error_message": null,
  "completed_at": "2025-09-20T05:30:15"
}
```

---

## 6. Get Stored Holdings

**Endpoint:** `GET /etf/holdings/{symbol}`
**Purpose:** Retrieve stored holdings data for an ETF

### Postman Setup:
- **Method:** GET
- **URL:** `http://127.0.0.1:8000/etf/holdings/UTINIFTETF`

### Expected Response:
```json
{
  "symbol": "UTINIFTETF",
  "name": "UTI Nifty 50 ETF",
  "isin": "INF789F1AZC0",
  "asset_class": "Equity",
  "market_cap_category": "Large cap",
  "holdings_count": 53,
  "holdings_fetched_at": "2025-09-20T05:30:15",
  "holdings": [
    {
      "stock_name": "HDFC Bank Ltd.",
      "isin_code": "INE040A01034",
      "percentage": 13.09,
      "market_value": 1250000.0,
      "quantity": 1000
    },
    {
      "stock_name": "ICICI Bank Ltd.",
      "isin_code": "INE090A01021",
      "percentage": 8.99,
      "market_value": 850000.0,
      "quantity": 850
    }
  ]
}
```

---

## Testing Workflow

### Recommended Testing Order:

1. **Start with Stats** - `GET /etf/stats`
   - Verify server is working
   - Check available data

2. **Search for ETF** - `GET /etf/search?query=nifty&limit=3`
   - Find specific ETFs to test with
   - Note the symbols for next steps

3. **Fetch Single ETF Holdings** - `POST /etf/fetch-holdings/UTINIFTETF`
   - Start a background job
   - Note the job_id from response

4. **Monitor Job Progress** - `GET /jobs/{job_id}/status`
   - Check every few seconds until completed
   - Verify successful completion

5. **Get Stored Holdings** - `GET /etf/holdings/UTINIFTETF`
   - Verify holdings were stored correctly
   - Check holdings data structure

6. **Fetch All Holdings (Limited)** - `POST /etf/fetch-all-holdings?limit=2`
   - Test bulk processing
   - Monitor with job status endpoint

---

## Webhook Testing

If you want to test callbacks:

1. Go to https://webhook.site/
2. Copy your unique URL
3. Use it as `callback_url` parameter in POST requests
4. Monitor webhook.site for callback notifications when jobs complete

---

## Sample ETF Symbols to Test With

Based on your existing data:
- `UTINIFTETF` - UTI Nifty 50 ETF (has ISIN)
- `UTISENSETF` - UTI BSE Sensex ETF (has ISIN)
- `QNIFTY` - Quantum Nifty 50 ETF (has ISIN)
- `BANKBETF` - Bajaj Finserv Nifty Bank ETF (has ISIN)

---

## Error Responses

### ETF Not Found (404):
```json
{
  "detail": "ETF not found: INVALID_SYMBOL"
}
```

### Missing ISIN (400):
```json
{
  "detail": "ETF SYMBOL does not have an ISIN"
}
```

### Job Not Found (404):
```json
{
  "detail": "Job not found: invalid_job_id"
}
```

---

## Notes

- All POST endpoints return immediately with job_id
- Use job status endpoint to monitor progress
- Holdings are stored in separate collection (preserves original ETF data)
- API includes rate limiting (1 second delay between moneycontrol calls)
- Webhook callbacks are sent when jobs complete (if callback_url provided)