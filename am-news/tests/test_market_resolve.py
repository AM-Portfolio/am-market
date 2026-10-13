import asyncio
from unittest.mock import AsyncMock, MagicMock, patch

import jwt

from am_news.adapters.market_data import MarketDataInstrumentResolver, _service_bearer
from am_news.settings import settings


def test_service_bearer_signs_hs256():
    with patch.object(settings, "jwt_secret", "unit-test-secret"):
        token = _service_bearer()
    assert token
    payload = jwt.decode(token, "unit-test-secret", algorithms=["HS256"])
    assert payload["sub"] == "am-news"
    assert payload["scopes"] == ["service"]


def test_resolve_sends_bearer_when_jwt_secret_set():
    captured: dict[str, object] = {}

    class _Resp:
        def raise_for_status(self) -> None:
            return None

        def json(self) -> dict:
            return {
                "results": [
                    {
                        "query": "RELIANCE",
                        "matches": [
                            {
                                "symbol": "RELIANCE",
                                "isin": "INE002A01018",
                                "instrumentKey": "NSE_EQ|INE002A01018",
                            }
                        ],
                    }
                ]
            }

    async def _post(url, json=None, headers=None):
        captured["url"] = url
        captured["headers"] = headers
        return _Resp()

    client = MagicMock()
    client.post = AsyncMock(side_effect=_post)
    client.__aenter__ = AsyncMock(return_value=client)
    client.__aexit__ = AsyncMock(return_value=False)

    async def _run():
        with patch.object(settings, "jwt_secret", "unit-test-secret"), patch(
            "am_news.adapters.market_data.httpx.AsyncClient", return_value=client
        ):
            return await MarketDataInstrumentResolver().resolve_nifty50()

    resolved = asyncio.run(_run())
    assert str(captured["headers"]["Authorization"]).startswith("Bearer ")
    assert any(item.symbol == "RELIANCE" for item in resolved)
