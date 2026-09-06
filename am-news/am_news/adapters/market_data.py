from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any

import httpx
import jwt

from am_news.domain.entities import Instrument
from am_news.domain.universe import NIFTY50_SYMBOLS
from am_news.settings import settings


def _service_bearer() -> str | None:
    secret = settings.jwt_secret
    if not secret:
        return None
    now = datetime.now(timezone.utc)
    return jwt.encode(
        {
            "sub": "am-news",
            "scopes": ["service"],
            "iat": int(now.timestamp()),
            "exp": int((now + timedelta(minutes=5)).timestamp()),
        },
        secret,
        algorithm="HS256",
    )


class MarketDataInstrumentResolver:
    async def resolve_nifty50(self) -> tuple[Instrument, ...]:
        url = f"{settings.market_data_url.rstrip('/')}/v1/securities/batch-search"
        payload = {"queries": list(NIFTY50_SYMBOLS), "limit": 1, "minMatchScore": 0.7}
        headers = {"Accept": "application/json"}
        bearer = _service_bearer()
        if bearer:
            headers["Authorization"] = f"Bearer {bearer}"
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.post(url, json=payload, headers=headers)
                response.raise_for_status()
                body = response.json()
        except Exception:
            return ()
        results = body.get("results") if isinstance(body, dict) else None
        if not isinstance(results, list):
            return ()
        by_query: dict[str, dict[str, Any]] = {}
        for row in results:
            if not isinstance(row, dict):
                continue
            query = str(row.get("query") or "").upper()
            matches = row.get("matches") or []
            if matches and isinstance(matches, list) and isinstance(matches[0], dict):
                by_query[query] = matches[0]
        resolved: list[Instrument] = []
        for symbol in NIFTY50_SYMBOLS:
            match = by_query.get(symbol)
            if not match:
                continue
            isin = str(match.get("isin") or "")
            instrument_key = str(
                match.get("instrumentKey")
                or match.get("instrument_key")
                or (f"NSE_EQ|{isin}" if isin else "")
            )
            if not instrument_key:
                continue
            resolved.append(
                Instrument(
                    symbol=str(match.get("symbol") or symbol).upper(),
                    isin=isin,
                    instrument_key=instrument_key,
                )
            )
        return tuple(resolved)
