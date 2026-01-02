"""ETF Holdings Service - Dedicated service for fetching and storing holdings data"""
import sys
from pathlib import Path
from typing import List, Optional
from datetime import datetime
import httpx
import asyncio
import os

sys.path.insert(0, str(Path(__file__).parent.parent))

from am_etf.holdings_models import ETFHoldingsData, ETFHoldingRecord


class ETFHoldingsService:
    """Service dedicated to fetching and storing ETF holdings data"""
    
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

    def _get_holdings_collection(self):
        if self._holdings_collection is None:
            import motor.motor_asyncio
            self._client = motor.motor_asyncio.AsyncIOMotorClient(self.mongo_uri)
            self._db = self._client[self.db_name]
            self._holdings_collection = self._db.etf_holdings  # New collection
            # Create indexes
            self._holdings_collection.create_index("isin", unique=True)
            self._holdings_collection.create_index("symbol")
            self._holdings_collection.create_index("fetched_at")
        return self._holdings_collection

    @property
    def holdings_collection(self):
        return self._get_holdings_collection()

    async def fetch_holdings_from_api(self, isin: str) -> Optional[List[ETFHoldingRecord]]:
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
                        
                print(f"✅ Fetched {len(holdings)} holdings for ISIN {isin}")
                return holdings
                
        except Exception as e:
            print(f"❌ Failed to fetch holdings for ISIN {isin}: {e}")
            return None
    
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

    async def fetch_and_store_holdings_for_isin(self, isin: str, symbol: str = None, etf_name: str = None) -> bool:
        """Fetch holdings for a specific ISIN and store in dedicated collection"""
        print(f"🔄 Fetching holdings for ISIN {isin} ({symbol or 'Unknown Symbol'})")
        
        holdings = await self.fetch_holdings_from_api(isin)
        
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
            print(f"✅ Stored {len(holdings)} holdings for {symbol or isin}")
            return True
        else:
            print(f"⚠️  No holdings found for {symbol or isin}")
            return False

    async def get_holdings_by_isin(self, isin: str) -> Optional[ETFHoldingsData]:
        """Get stored holdings data by ISIN"""
        col = self._get_holdings_collection()
        doc = await col.find_one({"isin": isin})
        if doc:
            doc.pop("_id", None)
            return ETFHoldingsData(**doc)
        return None

    async def list_all_holdings(self, limit: int = 10) -> List[ETFHoldingsData]:
        """List all stored holdings data"""
        col = self._get_holdings_collection()
        cursor = col.find().limit(limit).sort("fetched_at", -1)
        results = []
        async for doc in cursor:
            doc.pop("_id", None)
            results.append(ETFHoldingsData(**doc))
        return results

    async def get_holdings_stats(self):
        """Get statistics about stored holdings"""
        col = self._get_holdings_collection()
        total_count = await col.count_documents({})
        return {
            "total_etfs_with_holdings": total_count,
            "collection_name": "etf_holdings"
        }

    async def close(self):
        if self._client:
            self._client.close()


def create_etf_holdings_service(mongo_uri: str = "mongodb://admin:password123@localhost:27017", db_name: str = "etf_data") -> ETFHoldingsService:
    return ETFHoldingsService(mongo_uri, db_name)