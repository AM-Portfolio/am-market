"""ETF search and holdings lookup (domain layer)."""
from datetime import datetime, timedelta
from typing import List, Optional, Union

from am_api.schemas.etf import (
    EtfHoldingsLookupResponse,
    EtfSearchResponse,
    EtfSummary,
)
from am_configs.settings import settings
from am_etf.holdings_service import ETFHoldingsService
from am_etf.models import ETFInstrument
from am_etf.service import ETFService
from am_common.logging.request_logging import get_logger

_log = get_logger("etf_lookup")


def etf_dedupe_key(etf: dict) -> str:
    isin = (etf.get("isin") or "").strip()
    if isin:
        return f"isin:{isin}"
    symbol = (etf.get("symbol") or "").strip()
    if symbol:
        return f"symbol:{symbol.upper()}"
    return ""


def etf_to_summary(etf: ETFInstrument) -> EtfSummary:
    return EtfSummary(
        symbol=etf.symbol,
        name=etf.name,
        isin=etf.isin,
        asset_class=etf.asset_class,
        market_cap_category=etf.market_cap_category,
    )


def etf_holdings_payload(etf: ETFInstrument, holdings_data) -> dict:
    response = {
        "symbol": etf.symbol,
        "name": etf.name,
        "isin": etf.isin,
        "asset_class": etf.asset_class,
        "market_cap_category": etf.market_cap_category,
    }
    if holdings_data:
        response.update(
            {
                "holdings_count": holdings_data.total_holdings,
                "holdings_fetched_at": (
                    holdings_data.fetched_at.isoformat()
                    if holdings_data.fetched_at
                    else None
                ),
                "holdings": [
                    {
                        "stock_name": holding.stock_name,
                        "isin_code": holding.isin_code,
                        "percentage": holding.percentage,
                        "market_value": holding.market_value,
                        "quantity": holding.quantity,
                    }
                    for holding in holdings_data.holdings
                ],
            }
        )
    else:
        response["holdings"] = None
        response["message"] = "No holdings data available"
    return response


class EtfLookupService:
    def __init__(
        self,
        etf_service: ETFService,
        holdings_service: ETFHoldingsService,
    ):
        self._etf = etf_service
        self._holdings = holdings_service
        self._list_cache: List[ETFInstrument] = []
        self._list_cache_at: Optional[datetime] = None

    async def load_all_etfs(self) -> List[ETFInstrument]:
        cache_valid = False
        if self._list_cache and self._list_cache_at:
            age = datetime.now() - self._list_cache_at
            if age < timedelta(minutes=settings.etf_list_cache_minutes):
                cache_valid = True
        if cache_valid:
            return self._list_cache

        all_etfs = await self._etf.list(limit=2000)
        self._list_cache = all_etfs
        self._list_cache_at = datetime.now()
        return all_etfs

    async def search(self, query: str, limit: int = 10) -> EtfSearchResponse:
        all_etfs = await self.load_all_etfs()
        if not query or query == "*":
            subset = all_etfs[:limit]
            return EtfSearchResponse(
                query=query,
                total_found=len(subset),
                etfs=[etf_to_summary(e) for e in subset],
            )

        query_lower = query.lower().strip()
        matching: List[EtfSummary] = []
        for etf in all_etfs:
            if (
                (etf.symbol and query_lower in etf.symbol.lower())
                or (etf.name and query_lower in etf.name.lower())
                or (etf.isin and query_lower in etf.isin.lower())
            ):
                matching.append(etf_to_summary(etf))
                if len(matching) >= limit:
                    break

        return EtfSearchResponse(
            query=query,
            total_found=len(matching),
            etfs=matching,
        )

    async def _resolve_exact(self, input_val: str) -> Optional[ETFInstrument]:
        etf = await self._etf.get_by_symbol(input_val)
        if etf:
            return etf
        return await self._etf.get_by_isin(input_val)

    async def _search_instruments(
        self, query: str, limit: Optional[int] = None
    ) -> List[ETFInstrument]:
        all_etfs = await self.load_all_etfs()
        query_lower = query.lower().strip()
        if not query_lower:
            return []

        matching: List[ETFInstrument] = []
        for etf in all_etfs:
            if (
                (etf.symbol and query_lower in etf.symbol.lower())
                or (etf.name and query_lower in etf.name.lower())
                or (etf.isin and query_lower in etf.isin.lower())
            ):
                matching.append(etf)
                if limit and len(matching) >= limit:
                    break
        return matching

    async def _holdings_for_etfs(self, matching_etfs: List[ETFInstrument]) -> List[dict]:
        results = []
        for item in matching_etfs:
            h_data = (
                await self._holdings.get_holdings_by_isin(item.isin)
                if item.isin
                else None
            )
            results.append(etf_holdings_payload(item, h_data))
        return results

    async def resolve_holdings_by_query(
        self, query: str
    ) -> Union[dict, List[dict], None]:
        exact = await self._resolve_exact(query)
        if exact:
            h_data = (
                await self._holdings.get_holdings_by_isin(exact.isin)
                if exact.isin
                else None
            )
            return etf_holdings_payload(exact, h_data)

        matching = await self._search_instruments(query)
        if not matching:
            return None
        if len(matching) == 1:
            return (await self._holdings_for_etfs(matching))[0]
        _log.info(
            "holdings: query=%r matched %s ETFs (returning list)",
            query,
            len(matching),
        )
        return await self._holdings_for_etfs(matching)

    async def lookup_holdings(self, items: List[str]) -> EtfHoldingsLookupResponse:
        deduped_inputs = list(dict.fromkeys([s.strip() for s in items if s and s.strip()]))
        merged: List[dict] = []
        seen_keys: set[str] = set()
        not_found: List[str] = []

        for inp in deduped_inputs:
            resolved = await self.resolve_holdings_by_query(inp)
            if resolved is None:
                not_found.append(inp)
                continue

            candidates = resolved if isinstance(resolved, list) else [resolved]
            added_for_input = 0
            for etf in candidates:
                key = etf_dedupe_key(etf)
                if not key or key in seen_keys:
                    continue
                seen_keys.add(key)
                merged.append(etf)
                added_for_input += 1

            if added_for_input == 0 and candidates:
                _log.info(
                    "holdings: input=%r matched only ETFs already in response",
                    inp,
                )

        return EtfHoldingsLookupResponse(
            items=deduped_inputs,
            total_found=len(merged),
            etfs=merged,
            not_found=not_found,
        )
