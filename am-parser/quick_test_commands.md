# Quick ETF API Test Commands

## Test Server is Running
```bash
curl http://127.0.0.1:8000/etf/stats
```

## Search for ETFs
```bash
curl "http://127.0.0.1:8000/etf/search?query=nifty&limit=3"
```

## Fetch Holdings for Single ETF
```bash
curl -X POST http://127.0.0.1:8000/etf/fetch-holdings/UTINIFTETF
```

## Check Job Status (replace JOB_ID)
```bash
curl http://127.0.0.1:8000/jobs/JOB_ID/status
```

## Get Stored Holdings
```bash
curl http://127.0.0.1:8000/etf/holdings/UTINIFTETF
```

## Fetch All Holdings (Limited)
```bash
curl -X POST "http://127.0.0.1:8000/etf/fetch-all-holdings?limit=2"
```