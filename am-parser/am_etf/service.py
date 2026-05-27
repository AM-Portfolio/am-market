"""ETF persistence service"""
from typing import List, Optional, Iterable
from datetime import datetime
import asyncio
import random

from am_etf.models import ETFInstrument, ETFHolding
from am_etf.clients import enrich_holdings_with_isins, fetch_holdings_from_moneycontrol
from am_persistence.mongo_factory import get_async_mongo_client, release_service_mongo_refs


class ETFService:
    def __init__(self, mongo_uri: str = None, db_name: str = None):
        """Initialize ETF Service
        
        Args:
            mongo_uri: MongoDB URI (defaults to settings.mongo_uri)
            db_name: Database name (defaults to settings.effective_etf_db)
        """
        from am_configs.settings import settings, get_mongo_uri
        from am_common.logging.request_logging import get_logger

        self._log = get_logger("etf_service")
        self.mongo_uri = mongo_uri or get_mongo_uri()
        self.db_name = db_name or settings.effective_etf_db
        self._client = None
        self._db = None
        self._collection = None

    def _get_collection(self):
        if self._collection is None:
            mongo_target = (
                self.mongo_uri.split("@")[-1]
                if "@" in self.mongo_uri
                else self.mongo_uri
            )
            self._log.info("Mongo connect target=%s db=%s", mongo_target, self.db_name)
            self._client = get_async_mongo_client(self.mongo_uri)

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
        """Fetch holdings from Moneycontrol and enrich via market-data."""
        holdings = await fetch_holdings_from_moneycontrol(isin)
        if holdings:
            await enrich_holdings_with_isins(holdings)
        return holdings

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
        release_service_mongo_refs(self)

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
