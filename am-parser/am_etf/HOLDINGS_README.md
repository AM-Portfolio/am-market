# ETF Holdings Management System

This module provides separate storage and management of ETF holdings data without modifying the original ETF collection.

## Architecture

### Collections:
- **`etfs`** - Original ETF data collection (preserved/untouched)
- **`etf_holdings`** - New dedicated collection for holdings data

### Key Components:

#### Models
- `holdings_models.py` - Dedicated models for holdings data
  - `ETFHoldingRecord` - Individual holding/stock data
  - `ETFHoldingsData` - Complete holdings record for an ETF

#### Services
- `holdings_service.py` - Service for fetching and storing holdings
  - Fetches from moneycontrol API
  - Stores in dedicated `etf_holdings` collection
  - Does not modify original ETF documents

#### CLI Tools
- `fetch_equity_holdings.py` - Fetch holdings for equity ETFs
- `compare_collections.py` - Compare original vs holdings collections

## Usage

### Fetch Holdings for Equity ETFs
```bash
python -m am_etf.fetch_equity_holdings
```

### Query Stored Holdings
```bash
python -m am_etf.fetch_equity_holdings query
```

### Compare Collections
```bash
python -m am_etf.compare_collections
```

## Data Flow

1. **Original ETF Data**: Loaded from JSON into `etfs` collection (270 records)
2. **Holdings Fetching**: Separate process fetches holdings from API
3. **Holdings Storage**: Stored in dedicated `etf_holdings` collection
4. **Preservation**: Original ETF data remains completely unchanged

## Collections Comparison

| Collection | Purpose | Records | Holdings Storage |
|------------|---------|---------|------------------|
| `etfs` | Original ETF metadata | 270 ETFs | Some embedded (4 ETFs) |
| `etf_holdings` | Dedicated holdings | 2 ETFs | Separate dedicated storage |

## Benefits

- ✅ **Data Preservation**: Original ETF collection untouched
- ✅ **Separation of Concerns**: Holdings in dedicated collection
- ✅ **Scalability**: Can store holdings independently
- ✅ **Flexibility**: Different update cycles for ETF metadata vs holdings
- ✅ **Clean Architecture**: Clear data boundaries