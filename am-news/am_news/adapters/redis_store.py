from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any

from am_news.adapters.memory import AFFAIRS_KEY, LOCK_KEY, TOKEN_KEY
from am_news.settings import settings

try:
    from redis.asyncio import Redis
except ImportError:  # pragma: no cover
    Redis = None


class RedisStores:
    def __init__(self, host: str, port: int, password: str) -> None:
        if Redis is None:
            raise RuntimeError("redis is required for RedisStores")
        self._redis = Redis(host=host, port=port, password=password or None, decode_responses=True)

    async def get_affairs(self) -> dict[str, Any] | None:
        raw = await self._redis.get(AFFAIRS_KEY)
        return json.loads(raw) if raw else None

    async def set_affairs(self, payload: dict[str, Any], ttl_seconds: int) -> None:
        await self._redis.set(AFFAIRS_KEY, json.dumps(payload), ex=ttl_seconds)

    async def get_holdings(self, key: str) -> dict[str, Any] | None:
        raw = await self._redis.get(key)
        return json.loads(raw) if raw else None

    async def set_holdings(self, key: str, payload: dict[str, Any], ttl_seconds: int) -> None:
        await self._redis.set(key, json.dumps(payload), ex=ttl_seconds)

    async def affairs_age_seconds(self) -> int | None:
        raw = await self._redis.get(AFFAIRS_KEY)
        if not raw:
            return None
        payload = json.loads(raw)
        generated = payload.get("generated_at")
        if not generated:
            return None
        when = datetime.fromisoformat(generated)
        if when.tzinfo is None:
            when = when.replace(tzinfo=timezone.utc)
        return int((datetime.now(timezone.utc) - when).total_seconds())

    async def get_upstox_token(self) -> str | None:
        value = await self._redis.get(TOKEN_KEY)
        return value or None

    async def acquire(self) -> bool:
        return bool(await self._redis.set(LOCK_KEY, "1", nx=True, ex=settings.feed_lock_ttl_seconds))

    async def release(self) -> None:
        await self._redis.delete(LOCK_KEY)
