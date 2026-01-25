"""ETF persistence service"""
import sys
from pathlib import Path
from typing import List, Optional, Iterable
from datetime import datetime
import httpx
import asyncio
import random
import os

sys.path.insert(0, str(Path(__file__).parent.parent))

from am_etf.models import ETFInstrument, ETFHolding


class ETFService:
    def __init__(self, mongo_uri: str = None, db_name: str = None):
        """Initialize ETF Service
        
        Args:
            mongo_uri: MongoDB URI (defaults to settings.mongo_uri)
            db_name: Database name (defaults to settings.effective_etf_db)
        """
        from am_configs.settings import settings
        
        self.mongo_uri = mongo_uri or settings.mongo_uri
        self.db_name = db_name or settings.effective_etf_db
        self._client = None
        self._db = None
        self._collection = None

    def _get_collection(self):
        if self._collection is None:
            import motor.motor_asyncio
            self._client = motor.motor_asyncio.AsyncIOMotorClient(self.mongo_uri)
            self._db = self._client[self.db_name]
            self._collection = self._db.etfs
            # Indexes for lookup & uniqueness
            self._collection.create_index("symbol")
            self._collection.create_index("isin")
            self._collection.create_index([("symbol", 1), ("isin", 1)], unique=True, sparse=True)
        return self._collection

    @property
    def collection(self):
        return self._get_collection()

    async def fetch_holdings_from_api(self, isin: str) -> Optional[List[ETFHolding]]:
        """Fetch holdings data from moneycontrol API"""
        if not isin:
            return None
            
        url = f"https://mf.moneycontrol.com/service/etf/v1/getSchemeHoldingData?isin={isin}&key=Stocks"
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(url)
                response.raise_for_status()
                data = response.json()
                
                holdings = []
                # Parse the response structure - adjust based on actual API response
                if isinstance(data, dict) and 'data' in data:
                    holdings_data = data['data']
                elif isinstance(data, list):
                    holdings_data = data
                else:
                    holdings_data = data
                
                if isinstance(holdings_data, list):
                    for holding_data in holdings_data:
                        holding = ETFHolding(
                            stock_name=holding_data.get('name') or holding_data.get('stock_name'),
                            isin_code=holding_data.get('isin_code') or holding_data.get('isin'),
                            percentage=self._safe_float(holding_data.get('holdingPer') or holding_data.get('percentage') or holding_data.get('weight')),
                            market_value=self._safe_float(holding_data.get('investedAmount') or holding_data.get('market_value') or holding_data.get('value')),
                            quantity=self._safe_int(holding_data.get('quantity')),
                            raw_data=holding_data
                        )
                        holdings.append(holding)
                
                # Enrich holdings with ISINs from market-data
                if holdings:
                    await self.enrich_holdings_with_isins(holdings)
                        
                return holdings
                
        except Exception as e:
            return None

    async def enrich_holdings_with_isins(self, holdings: List[ETFHolding]):
        """
        Enrich holdings with ISINs using market-data batch search
        """
        try:
            import os
            
            # Filter holdings that need enrichment
            stock_names = [h.stock_name for h in holdings if h.stock_name]
            
            if not stock_names:
                return

            market_data_url = os.getenv("MARKET_DATA_URL", "http://localhost:8093")
            api_url = f"{market_data_url}/v1/securities/batch-search"
            
            async with httpx.AsyncClient(timeout=60.0) as client:
                # Process all at once since market-data handles 1000+
                payload = {
                    "queries": stock_names,
                    "limit": 1,
                    "minMatchScore": 0.7
                }
                
                try:
                    response = await client.post(api_url, json=payload)
                    response.raise_for_status()
                    result_data = response.json()
                    
                    # Process results
                    self._process_enrichment_results(holdings, result_data)
                    
                except Exception as req_err:
                    print(f"Error in batch search request: {req_err}")
                        
        except Exception as e:
            print(f"Enrichment failed: {e}")

    def _process_enrichment_results(self, holdings: List[ETFHolding], api_response: dict):
        """Map API results back to holdings records"""
        if not api_response or 'results' not in api_response:
            return

        results_map = {r.get('query'): r.get('matches', []) for r in api_response.get('results', [])}
        
        for holding in holdings:
            matches = results_map.get(holding.stock_name)
            
            if matches:
                # Get best match
                sorted_matches = sorted(matches, key=lambda x: x.get('matchScore', 0), reverse=True)
                
                if sorted_matches:
                    best_match = sorted_matches[0]
                    # Update holding with matched ISIN if score is good
                    if best_match.get('matchScore', 0) >= 0.8:
                        if not holding.isin_code:
                            holding.isin_code = best_match.get('isin')
                            # We could add extra fields to ETFHolding model if needed, 
                            # but core requirement is to get the ISIN
                            if hasattr(holding, 'enrichment_status'):
                                holding.enrichment_status = "MATCHED"
                                holding.matched_isin = best_match.get('isin')
                                holding.match_score = best_match.get('matchScore')
    
    def _safe_float(self, value) -> Optional[float]:
        """Safely convert to float"""
        if value is None:
            return None
        try:
            if isinstance(value, str):
                # Remove % sign if present
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

    async def upsert_etf(self, etf: ETFInstrument):
        col = self._get_collection()
        identifier = {"symbol": etf.symbol}
        if etf.isin:
            identifier["isin"] = etf.isin
        
        # Prepare update document without created_at for $set
        doc = etf.to_mongo_document()
        doc.pop("created_at", None)  # Remove created_at from $set to avoid conflict
        doc["updated_at"] = datetime.utcnow()
        
        await col.update_one(
            identifier,
            {"$set": doc, "$setOnInsert": {"created_at": datetime.utcnow()}},
            upsert=True
        )

    async def bulk_upsert(self, instruments: Iterable[ETFInstrument]) -> int:
        count = 0
        for inst in instruments:
            await self.upsert_etf(inst)
            count += 1
        return count

    async def list(self, limit: int = 100) -> List[ETFInstrument]:
        col = self._get_collection()
        cursor = col.find().limit(limit)
        out = []
        async for doc in cursor:
            doc.pop("_id", None)
            out.append(ETFInstrument(**doc))
        return out

    async def get_by_symbol(self, symbol: str) -> Optional[ETFInstrument]:
        col = self._get_collection()
        doc = await col.find_one({"symbol": symbol})
        if doc:
            doc.pop("_id", None)
            return ETFInstrument(**doc)
        return None

    async def get_by_isin(self, isin: str) -> Optional[ETFInstrument]:
        col = self._get_collection()
        doc = await col.find_one({"isin": isin})
        if doc:
            doc.pop("_id", None)
            return ETFInstrument(**doc)
        return None

    async def close(self):
        if self._client:
            self._client.close()

    async def fetch_and_update_holdings(self, limit: Optional[int] = None) -> int:
        """Fetch holdings for all ETFs with ISINs and update the database"""
        col = self._get_collection()
        
        # Find ETFs with ISINs that don't have holdings or have old holdings
        query = {"isin": {"$exists": True, "$ne": None}}
        cursor = col.find(query)
        if limit:
            cursor = cursor.limit(limit)
        
        updated_count = 0
        
        async for doc in cursor:
            isin = doc.get('isin')
            if not isin:
                continue
            holdings = await self.fetch_holdings_from_api(isin)
            
            if holdings:
                # Update the document with holdings
                await col.update_one(
                    {"_id": doc["_id"]},
                    {
                        "$set": {
                            "holdings": [h.dict() for h in holdings],
                            "holdings_fetched_at": datetime.utcnow(),
                            "updated_at": datetime.utcnow()
                        }
                    }
                )
                updated_count += 1
            
            # Add a random delay to be respectful to the API and look more natural
            delay = random.uniform(1.0, 3.0)
            await asyncio.sleep(delay)
        
        return updated_count

    async def get_etfs_with_holdings(self, limit: int = 10) -> List[ETFInstrument]:
        """Get ETFs that have holdings data"""
        col = self._get_collection()
        cursor = col.find({"holdings": {"$exists": True, "$ne": None}}).limit(limit)
        out = []
        async for doc in cursor:
            doc.pop("_id", None)
            out.append(ETFInstrument(**doc))
        return out

    async def get_etfs_by_asset_class(self, asset_class: str, limit: int = 10) -> List[ETFInstrument]:
        """Get ETFs filtered by asset class"""
        col = self._get_collection()
        cursor = col.find({"asset_class": asset_class}).limit(limit)
        out = []
        async for doc in cursor:
            doc.pop("_id", None)
            out.append(ETFInstrument(**doc))
        return out


def create_etf_service(mongo_uri: str = None, db_name: str = None) -> ETFService:
    """
    Factory to create ETF service instances
    
    Args:
        mongo_uri: MongoDB URI (defaults to settings.mongo_uri)
        db_name: Database name (defaults to settings.effective_etf_db)
    
    Returns:
        Configured ETFService instance
    """
    return ETFService(mongo_uri, db_name)
