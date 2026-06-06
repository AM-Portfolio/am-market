"""ETF holdings persistence and fetch orchestration."""
from datetime import datetime
from typing import List, Optional

from am_configs.settings import settings, get_mongo_uri
from am_etf.clients import enrich_holdings_with_isins, fetch_holdings_from_moneycontrol
from am_etf.holdings_models import ETFHoldingsData, ETFHoldingRecord
from am_persistence.mongo_factory import get_async_mongo_client, release_service_mongo_refs
from am_common.logging.request_logging import get_logger

_log = get_logger("etf_holdings")


class ETFHoldingsService:
    def __init__(self, mongo_uri: str = None, db_name: str = None):
        self.mongo_uri = mongo_uri or get_mongo_uri()
        self.db_name = db_name or settings.effective_etf_db
        self._client = None
        self._db = None
        self._holdings_collection = None

    def _get_holdings_collection(self):
        if self._holdings_collection is None:
            self._client = get_async_mongo_client(self.mongo_uri)

            self._db = self._client[self.db_name]
            self._holdings_collection = self._db.etf_holdings
            self._holdings_collection.create_index("isin", unique=True)
            self._holdings_collection.create_index("symbol")
            self._holdings_collection.create_index("fetched_at")
        return self._holdings_collection

    @property
    def holdings_collection(self):
        return self._get_holdings_collection()

    async def fetch_holdings_from_api(
        self, isin: str
    ) -> Optional[List[ETFHoldingRecord]]:
        holdings = await fetch_holdings_from_moneycontrol(isin)
        if holdings:
            await enrich_holdings_with_isins(holdings)
        return holdings

    async def store_holdings(self, holdings_data: ETFHoldingsData):
        col = self._get_holdings_collection()
        await col.replace_one(
            {"isin": holdings_data.isin},
            holdings_data.to_mongo_document(),
            upsert=True,
        )

    async def fetch_and_store_holdings_for_isin(
        self, isin: str, symbol: str = None, etf_name: str = None
    ) -> bool:
        _log.info("Fetching holdings for ISIN %s (%s)", isin, symbol or "unknown")
        holdings = await self.fetch_holdings_from_api(isin)
        if holdings:
            holdings_data = ETFHoldingsData(
                isin=isin,
                symbol=symbol,
                etf_name=etf_name,
                holdings=holdings,
                total_holdings=len(holdings),
                fetched_at=datetime.utcnow(),
            )
            await self.store_holdings(holdings_data)
            _log.info("Stored %s holdings for %s", len(holdings), symbol or isin)
            return True
        _log.warning("No holdings from API for %s", symbol or isin)
        return False

    async def get_holdings_by_isin(self, isin: str) -> Optional[ETFHoldingsData]:
        col = self._get_holdings_collection()
        doc = await col.find_one({"isin": isin})
        if doc:
            doc.pop("_id", None)
            return ETFHoldingsData(**doc)
        return None

    async def list_all_holdings(self, limit: int = 10) -> List[ETFHoldingsData]:
        col = self._get_holdings_collection()
        cursor = col.find().limit(limit).sort("fetched_at", -1)
        results = []
        async for doc in cursor:
            doc.pop("_id", None)
            results.append(ETFHoldingsData(**doc))
        return results

    async def get_holdings_stats(self):
        col = self._get_holdings_collection()
        total_count = await col.count_documents({})
        return {
            "total_etfs_with_holdings": total_count,
            "collection_name": "etf_holdings",
        }

    async def close(self):
        release_service_mongo_refs(self)


def create_etf_holdings_service(
    mongo_uri: str = None, db_name: str = None
) -> ETFHoldingsService:
    return ETFHoldingsService(mongo_uri, db_name)
