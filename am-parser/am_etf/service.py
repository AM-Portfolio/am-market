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
        # Use environment variables or defaults
        if mongo_uri is None:
            mongo_uri = os.getenv("MONGO_URI", "mongodb://admin:password123@localhost:27017")
        if db_name is None:
            db_name = os.getenv("MONGO_DB", "etf_data")
        self.mongo_uri = mongo_uri
        self.db_name = db_name
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
                        
                return holdings
                
        except Exception as e:
            return None
    
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


def create_etf_service(mongo_uri: str = "mongodb://admin:password123@localhost:27017", db_name: str = "etf_data") -> ETFService:
    return ETFService(mongo_uri, db_name)
