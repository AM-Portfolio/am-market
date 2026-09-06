from __future__ import annotations

from typing import Any

import httpx

from am_news.settings import settings


class UpstoxNewsAdapter:
    def __init__(self, token_provider) -> None:
        self._token_provider = token_provider

    async def fetch_page(self, instrument_keys: tuple[str, ...], page_number: int = 1) -> dict[str, Any]:
        token = await self._token_provider.get_upstox_token()
        if not token:
            raise RuntimeError("token_missing")
        params = {
            "category": "instrument_keys",
            "instrument_keys": ",".join(instrument_keys),
            "page_number": str(page_number),
            "page_size": "100",
        }
        headers = {
            "Accept": "application/json",
            "Authorization": f"Bearer {token}",
            "User-Agent": settings.upstox_user_agent,
        }
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(settings.upstox_news_url, params=params, headers=headers)
            response.raise_for_status()
            payload = response.json()
            if not isinstance(payload, dict):
                return {"status": "success", "data": {}}
            return payload
