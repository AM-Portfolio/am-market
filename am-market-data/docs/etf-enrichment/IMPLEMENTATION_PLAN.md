# ETF Holdings ISIN Enrichment - Implementation Plan
## (am-parser Service - Python)

> [!NOTE]
> **Service**: This plan is for **am-parser** (Python-based ETF parsing service)  
> **Dependency**: Calls **am-market-data** (Java) REST API for ISIN lookup

---

## Problem Statement

ETF holdings fetched from Moneycontrol API lack ISIN numbers:

```python
# Current ETFHoldingRecord structure
{
    "stock_name": "HDFC Bank Ltd.",
    "isin_code": None,  # ❌ NOT provided by API
    "percentage": 12.88,
    "market_value": 71699.08,
    "quantity": None
}
```

**Root Cause**: Moneycontrol API provides only company names in holdings data, not ISINs.

**Impact**:
- Cannot link holdings to Securities for real-time pricing
- Cannot perform portfolio-level analytics using ISIN
- Missing critical identifier for financial data joins

---

## Solution Architecture

### Flow:
1. **Fetch Holdings** → am-parser fetches from Moneycontrol API (stock_name only)
2. **Call ISIN Lookup** → HTTP GET to am-market-data REST API
3. **Match & Score** → Receive ISIN with confidence score
4. **Enrich Model** → Populate `isin_code` in ETFHoldingRecord
5. **Store** → Save enriched holdings to MongoDB (Motor async)

---

## Implementation

### 1. ISIN Enrichment Service

**File**: `am-parser/am_etf/isin_enrichment_service.py`

```python
"""ISIN Enrichment Service - Maps company names to ISINs via am-market-data API"""
import httpx
import os
from typing import Optional, List
from am_etf.holdings_models import ETFHoldingRecord


class ISINEnrichmentService:
    """Service to enrich holdings with ISIN by calling am-market-data API"""
    
    def __init__(self, market_data_url: str = None):
        # Default to environment variable or localhost
        self.market_data_url = market_data_url or os.getenv(
            "AM_MARKET_DATA_URL", 
            "http://localhost:8020"
        )
        self.lookup_endpoint = f"{self.market_data_url}/api/v1/securities/lookup-isin"
        self.min_confidence_score = float(os.getenv("ISIN_MIN_CONFIDENCE", "0.85"))
    
    async def lookup_isin(self, company_name: str) -> Optional[str]:
        """
        Lookup ISIN by company name using am-market-data API
        
        Args:
            company_name: Company name from ETF holding
            
        Returns:
            ISIN string if found with high confidence, None otherwise
        """
        if not company_name or not company_name.strip():
            return None
        
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(
                    self.lookup_endpoint,
                    params={"companyName": company_name}
                )
                response.raise_for_status()
                results = response.json()
                
                if results and len(results) > 0:
                    best_match = results[0]  # Already sorted by score
                    score = best_match.get("score", 0.0)
                    isin = best_match.get("isin")
                    
                    if score >= self.min_confidence_score and isin:
                        print(f"✅ ISIN match: {company_name} → {isin} (score: {score:.2f})")
                        return isin
                    else:
                        print(f"⚠️  Low confidence for {company_name}: {isin} (score: {score:.2f})")
                        return None
                else:
                    print(f"❌ No ISIN found for: {company_name}")
                    return None
                    
        except httpx.HTTPError as e:
            print(f"❌ HTTP error calling market-data API: {e}")
            return None
        except Exception as e:
            print(f"❌ Error in ISIN lookup for {company_name}: {e}")
            return None
    
    async def enrich_holding(self, holding: ETFHoldingRecord) -> ETFHoldingRecord:
        """
        Enrich a single holding with ISIN
        
        Args:
            holding: ETFHoldingRecord with stock_name
            
        Returns:
            Same holding with isin_code populated (if found)
        """
        # Skip if already has ISIN
        if holding.isin_code:
            return holding
        
        # Lookup ISIN
        isin = await self.lookup_isin(holding.stock_name)
        if isin:
            holding.isin_code = isin
        
        return holding
    
    async def enrich_holdings_batch(self, holdings: List[ETFHoldingRecord]) -> List[ETFHoldingRecord]:
        """
        Enrich multiple holdings with ISINs
        
        Args:
            holdings: List of ETFHoldingRecord objects
            
        Returns:
            List of enriched holdings
        """
        import asyncio
        
        # Enrich concurrently for better performance
        tasks = [self.enrich_holding(holding) for holding in holdings]
        enriched = await asyncio.gather(*tasks)
        
        # Statistics
        total = len(enriched)
        enriched_count = sum(1 for h in enriched if h.isin_code is not None)
        success_rate = (enriched_count / total * 100) if total > 0 else 0
        
        print(f"📊 Enrichment: {enriched_count}/{total} ({success_rate:.1f}%) holdings enriched with ISIN")
        
        return enriched


# Singleton instance
_enrichment_service = None

def get_isin_enrichment_service() -> ISINEnrichmentService:
    """Get or create singleton enrichment service instance"""
    global _enrichment_service
    if _enrichment_service is None:
        _enrichment_service = ISINEnrichmentService()
    return _enrichment_service
```

---

### 2. Integration into Existing ETFHoldingsService

**File**: `am-parser/am_etf/holdings_service.py` (MODIFY)

```python
# Add import at top
from am_etf.isin_enrichment_service import get_isin_enrichment_service

class ETFHoldingsService:
    # ... existing code ...
    
    async def fetch_and_store_holdings_for_isin(self, isin: str, symbol: str = None, etf_name: str = None) -> bool:
        """Fetch holdings for a specific ISIN and store in dedicated collection"""
        print(f"🔄 Fetching holdings for ISIN {isin} ({symbol or 'Unknown Symbol'})")
        
        # 1. Fetch from API
        holdings = await self.fetch_holdings_from_api(isin)
        
        if holdings:
            # 2. 🆕 ENRICH WITH ISIN
            enrichment_service = get_isin_enrichment_service()
            enriched_holdings = await enrichment_service.enrich_holdings_batch(holdings)
            
            # 3. Store enriched holdings
            holdings_data = ETFHoldingsData(
                isin=isin,
                symbol=symbol,
                etf_name=etf_name,
                holdings=enriched_holdings,  # 🆕 Now includes ISINs!
                total_holdings=len(enriched_holdings),
                fetched_at=datetime.utcnow()
            )
            
            await self.store_holdings(holdings_data)
            
            # 4. Log enrichment stats
            enriched_count = sum(1 for h in enriched_holdings if h.isin_code)
            print(f"✅ Stored {len(enriched_holdings)} holdings ({enriched_count} with ISIN) for {symbol or isin}")
            return True
        else:
            print(f"⚠️  No holdings found for {symbol or isin}")
            return False
```

---

### 3. Batch Enrichment Script

**File**: `am-parser/scripts/enrich_existing_holdings.py` (NEW)

See full script in separate file.

---

## Configuration

```bash
# .env or environment variables
AM_MARKET_DATA_URL=http://localhost:8020
ISIN_MIN_CONFIDENCE=0.85
MONGO_URI=mongodb://admin:password123@localhost:27017
MONGO_DB=mutual_funds
```

---

## Verification

### Test ISIN Lookup
```bash
curl "http://localhost:8020/api/v1/securities/lookup-isin?companyName=HDFC%20Bank"
```

### Run Enrichment
```bash
python -m scripts.enrich_existing_holdings --limit 10 --dry-run
```

---

## Timeline

- ISINEnrichmentService: 1 day
- Integration: 0.5 days
- Batch script: 1 day
- Testing: 1 day
- **Total**: 4 days
