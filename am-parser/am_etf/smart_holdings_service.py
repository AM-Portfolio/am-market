"""
Smart Caching Logic for ETF Holdings
Only fetches if:
1. ISIN not in etf_holdings collection
2. Data is older than X days (configurable)
3. Force refresh is requested
"""
from datetime import datetime, timedelta
from typing import Optional, List
import sys
import random
import os
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from am_etf.holdings_models import ETFHoldingsData, ETFHoldingRecord


class SmartETFHoldingsService:
    """ETF Holdings Service with intelligent caching"""
    
    def __init__(self, mongo_uri: str = None, db_name: str = None):
        # Use environment variables or defaults
        if mongo_uri is None:
            mongo_uri = os.getenv("MONGO_URI", "mongodb://admin:password123@localhost:27017")
        if db_name is None:
            db_name = os.getenv("MONGO_DB", "etf_data")
        self.mongo_uri = mongo_uri
        self.db_name = db_name
        self._client = None
        self._db = None
        self._holdings_collection = None
        
        # Cache configuration
        self.cache_expiry_days = 1  # Refresh data older than 1 day
        self.force_refresh = False
    
    def set_cache_policy(self, expiry_days: int = 1, force_refresh: bool = False):
        """Configure caching behavior"""
        self.cache_expiry_days = expiry_days
        self.force_refresh = force_refresh
    
    def _get_holdings_collection(self):
        if self._holdings_collection is None:
            import motor.motor_asyncio
            self._client = motor.motor_asyncio.AsyncIOMotorClient(self.mongo_uri)
            self._db = self._client[self.db_name]
            self._holdings_collection = self._db.etf_holdings
            # Create indexes
            self._holdings_collection.create_index("isin", unique=True)
            self._holdings_collection.create_index("fetched_at")
        return self._holdings_collection

    @property
    def holdings_collection(self):
        return self._get_holdings_collection()

    async def should_fetch_holdings(self, isin: str) -> tuple[bool, Optional[ETFHoldingsData]]:
        """
        Determine if holdings should be fetched for an ISIN
        Returns: (should_fetch: bool, existing_data: Optional[ETFHoldingsData])
        """
        # Check if data exists
        existing_data = await self.get_holdings_by_isin(isin)
        
        if not existing_data:
            # No data exists - definitely fetch
            return True, None
        
        if self.force_refresh:
            # Force refresh requested
            return True, existing_data
        
        # Check if data is stale
        if existing_data.fetched_at:
            age = datetime.utcnow() - existing_data.fetched_at
            if age.days >= self.cache_expiry_days:
                return True, existing_data
            else:
                return False, existing_data
        else:
            # No fetched_at timestamp - assume stale
            return True, existing_data

    async def fetch_holdings_from_api(self, isin: str) -> Optional[List[ETFHoldingRecord]]:
        """Fetch holdings data from moneycontrol API"""
        if not isin:
            return None
            
        url = f"https://mf.moneycontrol.com/service/etf/v1/getSchemeHoldingData?isin={isin}&key=Stocks"
        
        try:
            import httpx
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(url)
                response.raise_for_status()
                data = response.json()
                
                holdings = []
                # Parse the response structure
                if isinstance(data, dict) and 'data' in data:
                    holdings_data = data['data']
                elif isinstance(data, list):
                    holdings_data = data
                else:
                    holdings_data = data
                
                if isinstance(holdings_data, list):
                    for holding_data in holdings_data:
                        holding = ETFHoldingRecord(
                            stock_name=holding_data.get('name') or holding_data.get('stock_name', 'Unknown'),
                            isin_code=holding_data.get('isin_code') or holding_data.get('isin'),
                            percentage=self._safe_float(holding_data.get('holdingPer') or holding_data.get('percentage')),
                            market_value=self._safe_float(holding_data.get('investedAmount') or holding_data.get('market_value')),
                            quantity=self._safe_int(holding_data.get('quantity')),
                            raw_data=holding_data
                        )
                        holdings.append(holding)
                
                # Enrich holdings with ISINs from market-data
                if holdings:
                    await self.enrich_holdings_with_isins(holdings)
                        
                return holdings
                
        except Exception as e:
            # log error but return what we have or None
            print(f"Error fetching holdings: {e}") 
            return None

    async def enrich_holdings_with_isins(self, holdings: List[ETFHoldingRecord]):
        """
        Enrich holdings with ISINs using market-data batch search
        """
        try:
            import httpx
            import os
            
            # Filter holdings that need enrichment (missing ISIN or basic check)
            # We enrich everything to ensure we have the correct ISIN/Symbol mapping
            stock_names = [h.stock_name for h in holdings if h.stock_name]
            
            if not stock_names:
                return

            market_data_url = os.getenv("MARKET_DATA_URL", "http://localhost:8093")
            api_url = f"{market_data_url}/v1/securities/batch-search"
            
            # Process in chunks if needed, but market-data handles 1000, so we likely send all
            # Just in case, let's limit to 500 per call to be safe
            chunk_size = 500
            
            async with httpx.AsyncClient(timeout=60.0) as client:
                for i in range(0, len(stock_names), chunk_size):
                    chunk_names = stock_names[i:i + chunk_size]
                    
                    payload = {
                        "queries": chunk_names,
                        "limit": 3,  # Get top 3 matches to pick best
                        "minMatchScore": 0.7  # Fairly loose to catch variations, we filter better locally
                    }
                    
                    try:
                        response = await client.post(api_url, json=payload)
                        response.raise_for_status()
                        result_data = response.json()
                        
                        # Process results and map back to holdings
                        self._process_enrichment_results(holdings, result_data)
                        
                    except Exception as req_err:
                        print(f"Error in batch search request: {req_err}")
                        
        except Exception as e:
            print(f"Enrichment failed: {e}")
            # We don't fail the whole fetch if enrichment fails, just log it

    def _process_enrichment_results(self, holdings: List[ETFHoldingRecord], api_response: dict):
        """Map API results back to holdings records"""
        if not api_response or 'results' not in api_response:
            return

        # Create a map of query -> valid matches for quick lookup
        # The API preserves order usually, but let's be safe. 
        # API returns: results: [{query: "Reliance", matches: [...]}, ...]
        
        results_map = {r.get('query'): r.get('matches', []) for r in api_response.get('results', [])}
        
        # DEBUG LOGGING for enrichment
        print(f"Batch Search: Sent {len(holdings)} queries. Received {len(results_map)} results.")
        # Debug "Reliance" specifically
        reliance_keys = [k for k in results_map.keys() if "Reliance" in k]
        if reliance_keys:
             print(f"DEBUG keys for Reliance: {reliance_keys}")
             # Print matches for first reliance
             print(f"DEBUG matches for {reliance_keys[0]}: {results_map[reliance_keys[0]]}")
        
        for holding in holdings:
            matches = results_map.get(holding.stock_name)
            
            if matches:
                # Logic to pick the best match
                # 1. Exact ISIN match (if holding already had one) - unlikely if we are here to enrich
                # 2. Highest match Score
                # 3. Preference for 'COMPANY_NAME' or 'SYMBOL' match
                
                best_match = None
                
                # Sort matches by score descending
                sorted_matches = sorted(matches, key=lambda x: x.get('matchScore', 0), reverse=True)
                
                # Simply pick the top one for now if score is decent
                if sorted_matches:
                    for match in sorted_matches:
                        if match.get('matchScore', 0) < 0.6:
                            break # Score too low, stop looking
                            
                        candidate_isin = match.get('isin')
                        # Filter invalid ISINs
                        if not candidate_isin or candidate_isin == "-" or len(candidate_isin) < 10:
                            continue
                            
                        # Valid match found
                        best_match = match
                        break
                
                if best_match:
                    # found valid enriched ISIN
                    # Update primary fields directly as requested
                    enriched_isin = best_match.get('isin')
                    
                    # Store enrichment metadata but update primary ISIN
                    holding.isin_code = enriched_isin
                    holding.matched_isin = enriched_isin # Keep for provenance/debugging if needed, or remove if strict
                    holding.matched_symbol = best_match.get('symbol')
                    holding.match_score = best_match.get('matchScore')
                    holding.enrichment_status = "MATCHED"
                else:
                    holding.enrichment_status = "NO_MATCH"
            else:
                holding.enrichment_status = "NO_MATCH"
    
    def _safe_float(self, value) -> Optional[float]:
        """Safely convert to float"""
        if value is None:
            return None
        try:
            if isinstance(value, str):
                value = value.replace('%', '').strip()
            return float(value)
        except (ValueError, TypeError):
            return None
    
    def _safe_int(self, value) -> Optional[int]:
        """Safely convert to int"""
        if value is None:
            return None
        try:
            return int(value)
        except (ValueError, TypeError):
            return None

    async def store_holdings(self, holdings_data: ETFHoldingsData):
        """Store holdings data in dedicated collection"""
        col = self._get_holdings_collection()
        await col.replace_one(
            {"isin": holdings_data.isin},
            holdings_data.to_mongo_document(),
            upsert=True
        )

    async def smart_fetch_and_store_holdings(self, isin: str, symbol: str = None, etf_name: str = None) -> dict:
        """
        Smart fetch with caching logic
        Returns detailed result with cache status
        """
        result = {
            "isin": isin,
            "symbol": symbol,
            "cache_hit": False,
            "api_called": False,
            "success": False,
            "reason": "",
            "holdings_count": 0
        }
        
        # Check if we should fetch
        should_fetch, existing_data = await self.should_fetch_holdings(isin)
        
        if not should_fetch and existing_data:
            # Use cached data
            result.update({
                "cache_hit": True,
                "success": True,
                "reason": "Using cached data",
                "holdings_count": existing_data.total_holdings,
                "fetched_at": existing_data.fetched_at.isoformat() if existing_data.fetched_at else None
            })
            return result
        
        # Fetch from API
        holdings = await self.fetch_holdings_from_api(isin)
        result["api_called"] = True
        
        if holdings:
            holdings_data = ETFHoldingsData(
                isin=isin,
                symbol=symbol,
                etf_name=etf_name,
                holdings=holdings,
                total_holdings=len(holdings),
                fetched_at=datetime.utcnow()
            )
            
            await self.store_holdings(holdings_data)
            result.update({
                "success": True,
                "reason": "Fresh data fetched and stored",
                "holdings_count": len(holdings)
            })
        else:
            result.update({
                "success": False,
                "reason": "No holdings data available from API"
            })
        
        return result

    async def get_holdings_by_isin(self, isin: str) -> Optional[ETFHoldingsData]:
        """Get stored holdings data by ISIN"""
        col = self._get_holdings_collection()
        doc = await col.find_one({"isin": isin})
        if doc:
            doc.pop("_id", None)
            return ETFHoldingsData(**doc)
        return None

    async def get_cache_statistics(self) -> dict:
        """Get caching statistics"""
        col = self._get_holdings_collection()
        
        total_records = await col.count_documents({})
        
        # Count fresh records (today)
        today_start = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
        fresh_records = await col.count_documents({
            "fetched_at": {"$gte": today_start}
        })
        
        # Count stale records
        stale_cutoff = datetime.utcnow() - timedelta(days=self.cache_expiry_days)
        stale_records = await col.count_documents({
            "fetched_at": {"$lt": stale_cutoff}
        })
        
        return {
            "total_cached_records": total_records,
            "fresh_records_today": fresh_records,
            "stale_records": stale_records,
            "cache_expiry_days": self.cache_expiry_days,
            "cache_hit_potential": f"{((total_records - stale_records) / max(total_records, 1) * 100):.1f}%"
        }

    async def bulk_smart_fetch(self, etfs_with_isin: List, progress_callback=None) -> dict:
        """
        Bulk fetch with smart caching
        Returns summary of cache hits vs API calls
        """
        summary = {
            "total_processed": 0,
            "cache_hits": 0,
            "api_calls": 0,
            "successful_fetches": 0,
            "failed_fetches": 0,
            "results": []
        }
        
        for i, etf in enumerate(etfs_with_isin):
            result = await self.smart_fetch_and_store_holdings(
                isin=etf.isin,
                symbol=etf.symbol,
                etf_name=etf.name
            )
            
            summary["total_processed"] += 1
            summary["results"].append(result)
            
            if result["cache_hit"]:
                summary["cache_hits"] += 1
            if result["api_called"]:
                summary["api_calls"] += 1
            if result["success"]:
                summary["successful_fetches"] += 1
            else:
                summary["failed_fetches"] += 1
            
            # Progress callback
            if progress_callback:
                await progress_callback(i + 1, len(etfs_with_isin), result)
            
            # Only add delay if we called the API
            if result["api_called"]:
                import asyncio
                # Random delay between 1-3 seconds to make requests look more natural
                delay = random.uniform(1.0, 3.0)
                await asyncio.sleep(delay)
        
        # Calculate efficiency
        if summary["total_processed"] > 0:
            summary["cache_hit_rate"] = f"{(summary['cache_hits'] / summary['total_processed'] * 100):.1f}%"
            summary["api_call_savings"] = f"Saved {summary['cache_hits']} API calls"
        
        return summary

    async def close(self):
        if self._client:
            self._client.close()


def create_smart_etf_holdings_service(mongo_uri: str = None, db_name: str = None) -> SmartETFHoldingsService:
    """Factory function for smart ETF holdings service (defaults to settings)"""
    return SmartETFHoldingsService(mongo_uri, db_name)