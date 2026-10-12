from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any

from am_news.adapters.memory import (
    AFFAIRS_KEY,
    HOLDINGS_KEY_PREFIX,
    LOCK_KEY,
    SYMBOLS_LOCK_KEY,
    TOKEN_KEY,
)
from am_news.settings import settings

try:
    from redis.asyncio import Redis
except ImportError:  # pragma: no cover
    Redis = None


class RedisStores:
    def __init__(
        self,
        host: str,
        port: int,
        password: str,
        username: str = "",
        *,
        lock_key: str = LOCK_KEY,
    ) -> None:
        if Redis is None:
            raise RuntimeError("redis is required for RedisStores")
        self._lock_key = lock_key
        self._redis = Redis(
            host=host,
            port=port,
            username=username or None,
            password=password or None,
            decode_responses=True,
        )

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

    async def clear_holdings(self) -> None:
        cursor = 0
        while True:
            cursor, keys = await self._redis.scan(
                cursor=cursor, match=f"{HOLDINGS_KEY_PREFIX}*", count=100
            )
            if keys:
                await self._redis.delete(*keys)
            if cursor == 0:
                break

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
        return value or settings.upstox_access_token or None

    async def acquire(self) -> bool:
        return bool(
            await self._redis.set(
                self._lock_key, "1", nx=True, ex=settings.feed_lock_ttl_seconds
            )
        )

    async def release(self) -> None:
        await self._redis.delete(self._lock_key)


class RedisSymbolsLock:
    """Holdings-symbol ingest lock — separate from NIFTY feed lock."""

    def __init__(self, redis_client) -> None:
        self._redis = redis_client

    async def acquire(self) -> bool:
        return bool(
            await self._redis.set(
                SYMBOLS_LOCK_KEY, "1", nx=True, ex=settings.feed_lock_ttl_seconds
            )
        )

    async def release(self) -> None:
        await self._redis.delete(SYMBOLS_LOCK_KEY)
