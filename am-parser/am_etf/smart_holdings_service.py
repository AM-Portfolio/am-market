"""
Smart caching for ETF holdings — composes ETFHoldingsService (no duplicated fetch/Mongo code).
"""
import asyncio
import random
from datetime import datetime, timedelta
from typing import List, Optional

from am_configs.settings import settings
from am_etf.holdings_models import ETFHoldingsData
from am_etf.holdings_service import ETFHoldingsService, create_etf_holdings_service
from am_common.logging.request_logging import get_logger

_log = get_logger("smart_etf_holdings")


class SmartETFHoldingsService:
    """ETF Holdings Service with intelligent caching"""

    def __init__(
        self,
        holdings_service: Optional[ETFHoldingsService] = None,
        cache_expiry_days: Optional[int] = None,
        force_refresh: bool = False,
    ):
        self._holdings = holdings_service or create_etf_holdings_service()
        self.cache_expiry_days = (
            cache_expiry_days
            if cache_expiry_days is not None
            else settings.etf_holdings_cache_days
        )
        self.force_refresh = force_refresh


    def set_cache_policy(self, expiry_days: int = 1, force_refresh: bool = False):
        self.cache_expiry_days = expiry_days
        self.force_refresh = force_refresh

    @property
    def holdings_collection(self):
        return self._holdings.holdings_collection

    async def should_fetch_holdings(
        self, isin: str
    ) -> tuple[bool, Optional[ETFHoldingsData]]:
        existing_data = await self._holdings.get_holdings_by_isin(isin)
        if not existing_data:
            return True, None
        if self.force_refresh:
            return True, existing_data
        if existing_data.fetched_at:
            age = datetime.utcnow() - existing_data.fetched_at
            if age.days >= self.cache_expiry_days:
                return True, existing_data
            return False, existing_data
        return True, existing_data

    async def fetch_holdings_from_api(self, isin: str):
        return await self._holdings.fetch_holdings_from_api(isin)

    async def store_holdings(self, holdings_data: ETFHoldingsData):
        await self._holdings.store_holdings(holdings_data)

    async def get_holdings_by_isin(self, isin: str) -> Optional[ETFHoldingsData]:
        return await self._holdings.get_holdings_by_isin(isin)

    async def smart_fetch_and_store_holdings(
        self, isin: str, symbol: str = None, etf_name: str = None
    ) -> dict:
        result = {
            "isin": isin,
            "symbol": symbol,
            "cache_hit": False,
            "api_called": False,
            "success": False,
            "reason": "",
            "holdings_count": 0,
        }

        should_fetch, existing_data = await self.should_fetch_holdings(isin)
        if not should_fetch and existing_data:
            result.update(
                {
                    "cache_hit": True,
                    "success": True,
                    "reason": "Using cached data",
                    "holdings_count": existing_data.total_holdings,
                    "fetched_at": (
                        existing_data.fetched_at.isoformat()
                        if existing_data.fetched_at
                        else None
                    ),
                }
            )
            return result

        holdings = await self._holdings.fetch_holdings_from_api(isin)
        result["api_called"] = True

        if holdings:
            holdings_data = ETFHoldingsData(
                isin=isin,
                symbol=symbol,
                etf_name=etf_name,
                holdings=holdings,
                total_holdings=len(holdings),
                fetched_at=datetime.utcnow(),
            )
            await self._holdings.store_holdings(holdings_data)
            result.update(
                {
                    "success": True,
                    "reason": "Fresh data fetched and stored",
                    "holdings_count": len(holdings),
                }
            )
        else:
            result.update(
                {"success": False, "reason": "No holdings data available from API"}
            )
        return result

    async def get_cache_statistics(self) -> dict:
        col = self._holdings.holdings_collection
        total_records = await col.count_documents({})
        today_start = datetime.utcnow().replace(
            hour=0, minute=0, second=0, microsecond=0
        )
        fresh_records = await col.count_documents(
            {"fetched_at": {"$gte": today_start}}
        )
        stale_cutoff = datetime.utcnow() - timedelta(days=self.cache_expiry_days)
        stale_records = await col.count_documents(
            {"fetched_at": {"$lt": stale_cutoff}}
        )
        return {
            "total_cached_records": total_records,
            "fresh_records_today": fresh_records,
            "stale_records": stale_records,
            "cache_expiry_days": self.cache_expiry_days,
            "cache_hit_potential": (
                f"{((total_records - stale_records) / max(total_records, 1) * 100):.1f}%"
            ),
        }

    async def bulk_smart_fetch(self, etfs_with_isin: List, progress_callback=None) -> dict:
        summary = {
            "total_processed": 0,
            "cache_hits": 0,
            "api_calls": 0,
            "successful_fetches": 0,
            "failed_fetches": 0,
            "results": [],
        }

        for i, etf in enumerate(etfs_with_isin):
            result = await self.smart_fetch_and_store_holdings(
                isin=etf.isin,
                symbol=etf.symbol,
                etf_name=etf.name,
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
            if progress_callback:
                await progress_callback(i + 1, len(etfs_with_isin), result)
            if result["api_called"]:
                await asyncio.sleep(random.uniform(1.0, 3.0))

        if summary["total_processed"] > 0:
            summary["cache_hit_rate"] = (
                f"{(summary['cache_hits'] / summary['total_processed'] * 100):.1f}%"
            )
            summary["api_call_savings"] = f"Saved {summary['cache_hits']} API calls"
        return summary

    async def close(self):
        await self._holdings.close()


def create_smart_etf_holdings_service(
    mongo_uri: str = None, db_name: str = None
) -> SmartETFHoldingsService:
    return SmartETFHoldingsService(
        holdings_service=create_etf_holdings_service(mongo_uri, db_name)
    )
