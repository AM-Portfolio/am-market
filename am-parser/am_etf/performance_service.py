"""ETF fund performance SoT: charts → returns/sparkline with L1+Mongo cache."""
from __future__ import annotations

from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple

from am_api.schemas.fund_v2 import (
    FundPerformanceResult,
    ProductType,
)
from am_common.logging.request_logging import get_logger
from am_configs.settings import get_mongo_uri, settings
from am_etf.clients.market_data_client import fetch_historical_chart_points
from am_etf.performance_math import compute_performance_from_points
from am_persistence.mongo_factory import get_async_mongo_client, release_service_mongo_refs

_log = get_logger("fund_performance")

_NULL_PERF = {
    "return1Y": None,
    "return3Y": None,
    "return5Y": None,
    "sparklineCloses": None,
    "returnsAsOf": None,
}


class FundPerformanceService:
    def __init__(self, mongo_uri: str = None, db_name: str = None):
        self.mongo_uri = mongo_uri or get_mongo_uri()
        self.db_name = db_name or settings.effective_etf_db
        self._client = None
        self._col = None
        # L1: symbol -> (expires_at, perf_dict)
        self._l1: Dict[str, Tuple[datetime, dict]] = {}

    def _collection(self):
        if self._col is None:
            self._client = get_async_mongo_client(self.mongo_uri)
            db = self._client[self.db_name]
            self._col = db.fund_performance
            self._col.create_index("symbol", unique=True)
            self._col.create_index("computed_at")
        return self._col

    async def close(self):
        release_service_mongo_refs(self)
        self._col = None
        self._l1.clear()

    def _ttl(self) -> timedelta:
        hours = max(1, int(settings.fund_performance_cache_hours or 6))
        return timedelta(hours=hours)

    def _l1_get(self, symbol: str) -> Optional[dict]:
        entry = self._l1.get(symbol)
        if not entry:
            return None
        expires, payload = entry
        if datetime.utcnow() >= expires:
            self._l1.pop(symbol, None)
            return None
        return payload

    def _l1_put(self, symbol: str, payload: dict) -> None:
        self._l1[symbol] = (datetime.utcnow() + self._ttl(), payload)

    async def _mongo_get(self, symbol: str) -> Optional[dict]:
        try:
            col = self._collection()
            doc = await col.find_one({"symbol": symbol})
            if not doc:
                return None
            computed = doc.get("computed_at")
            if isinstance(computed, datetime):
                if datetime.utcnow() - computed > self._ttl():
                    return None
            return {
                "return1Y": doc.get("return1Y"),
                "return3Y": doc.get("return3Y"),
                "return5Y": doc.get("return5Y"),
                "sparklineCloses": doc.get("sparklineCloses"),
                "returnsAsOf": doc.get("returnsAsOf"),
            }
        except Exception as e:
            _log.warning("fund_performance mongo get failed: %s", e)
            return None

    async def _mongo_put(self, symbol: str, payload: dict) -> None:
        try:
            col = self._collection()
            await col.replace_one(
                {"symbol": symbol},
                {
                    "symbol": symbol,
                    "productType": ProductType.ETF.value,
                    "computed_at": datetime.utcnow(),
                    **payload,
                },
                upsert=True,
            )
        except Exception as e:
            _log.warning("fund_performance mongo put failed: %s", e)

    async def _compute_live(self, symbol: str) -> dict:
        timeout = float(settings.fund_performance_chart_timeout_s or 20.0)
        points = await fetch_historical_chart_points(
            symbol,
            range_value="5Y",
            is_index_symbol=False,
            timeout_s=timeout,
        )
        if not points:
            return dict(_NULL_PERF)
        return compute_performance_from_points(points)

    async def get_performance(
        self, symbol: str, *, force_refresh: bool = False
    ) -> dict:
        """Fail-open: always returns a dict (values may be null)."""
        key = (symbol or "").strip().upper()
        if not key:
            return dict(_NULL_PERF)

        if not force_refresh:
            cached = self._l1_get(key)
            if cached is not None:
                return dict(cached)
            cached = await self._mongo_get(key)
            if cached is not None:
                self._l1_put(key, cached)
                return dict(cached)

        try:
            payload = await self._compute_live(key)
        except Exception as e:
            _log.warning("performance compute failed for %s: %s", key, e)
            payload = dict(_NULL_PERF)

        self._l1_put(key, payload)
        await self._mongo_put(key, payload)
        return dict(payload)

    async def get_performance_batch(
        self, symbols: List[str], *, force_refresh: bool = False
    ) -> List[FundPerformanceResult]:
        results: List[FundPerformanceResult] = []
        seen = set()
        for raw in symbols:
            sym = (raw or "").strip().upper()
            if not sym or sym in seen:
                continue
            seen.add(sym)
            perf = await self.get_performance(sym, force_refresh=force_refresh)
            results.append(
                FundPerformanceResult(
                    productType=ProductType.ETF,
                    symbol=sym,
                    return1Y=perf.get("return1Y"),
                    return3Y=perf.get("return3Y"),
                    return5Y=perf.get("return5Y"),
                    returnsAsOf=perf.get("returnsAsOf"),
                    sparklineCloses=perf.get("sparklineCloses"),
                )
            )
        return results


def create_fund_performance_service() -> FundPerformanceService:
    return FundPerformanceService()
