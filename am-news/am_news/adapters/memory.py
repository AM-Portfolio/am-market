from __future__ import annotations

import hashlib
from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from am_news.domain.entities import (
    FeedState,
    Instrument,
    NewsArticle,
    ProcessStatus,
    RawBatch,
)
from am_news.settings import settings


def _norm(symbol: str) -> str:
    return symbol.strip().upper()


class MemoryArticleStore:
    def __init__(self) -> None:
        self._articles: dict[tuple[str, str], NewsArticle] = {}

    def put(self, articles: tuple[NewsArticle, ...]) -> None:
        for article in articles:
            key = (article.provider, article.article_uid)
            existing = self._articles.get(key)
            if existing is None:
                self._articles[key] = article
                continue
            merged = {item.instrument_key: item for item in existing.symbols}
            for item in article.symbols:
                merged[item.instrument_key] = item
            self._articles[key] = existing.model_copy(update={"symbols": tuple(merged.values())})

    async def upsert_articles(self, articles: tuple[NewsArticle, ...]) -> None:
        self.put(articles)

    async def latest_for_universe(self, symbols: tuple[str, ...], limit: int) -> tuple[NewsArticle, ...]:
        wanted = {_norm(s) for s in symbols}
        matched = [
            article
            for article in self._articles.values()
            if any(_norm(item.symbol) in wanted for item in article.symbols)
        ]
        matched.sort(key=lambda a: a.published_at, reverse=True)
        return tuple(matched[:limit])

    async def latest_for_symbols(self, symbols: tuple[str, ...], limit: int) -> tuple[NewsArticle, ...]:
        return await self.latest_for_universe(symbols, limit)


class MemoryRawStore:
    def __init__(self) -> None:
        self._rows: dict[str, RawBatch] = {}

    def put(self, batch: RawBatch) -> str:
        raw_id = batch.raw_id or uuid4().hex
        stored = batch.model_copy(
            update={"raw_id": raw_id, "created_at": batch.created_at or datetime.now(timezone.utc)}
        )
        self._rows[raw_id] = stored
        return raw_id

    async def insert(self, batch: RawBatch) -> str:
        return self.put(batch)

    async def get(self, raw_id: str) -> RawBatch | None:
        return self._rows.get(raw_id)

    async def list_recent(self, limit: int = 50) -> tuple[RawBatch, ...]:
        rows = sorted(self._rows.values(), key=lambda r: r.created_at or datetime.min, reverse=True)
        return tuple(rows[:limit])

    async def claim_next(self, statuses: tuple[ProcessStatus, ...]) -> RawBatch | None:
        for row in self._rows.values():
            if row.process_status in statuses:
                updated = row.model_copy(
                    update={"process_status": ProcessStatus.processing, "attempts": row.attempts + 1}
                )
                self._rows[row.raw_id] = updated
                return updated
        return None

    async def mark(self, raw_id: str, status: ProcessStatus, error: str | None = None) -> None:
        row = self._rows.get(raw_id)
        if row is None:
            return
        self._rows[raw_id] = row.model_copy(update={"process_status": status, "error": error})

    async def dead_count(self) -> int:
        return sum(1 for row in self._rows.values() if row.process_status == ProcessStatus.dead)


class MemorySnapshotCache:
    def __init__(self) -> None:
        self.affairs: dict[str, Any] | None = None
        self.holdings: dict[str, dict[str, Any]] = {}
        self._affairs_at: datetime | None = None

    async def get_affairs(self) -> dict[str, Any] | None:
        return self.affairs

    async def set_affairs(self, payload: dict[str, Any], ttl_seconds: int) -> None:
        del ttl_seconds
        self.affairs = payload
        self._affairs_at = datetime.now(timezone.utc)

    async def get_holdings(self, key: str) -> dict[str, Any] | None:
        return self.holdings.get(key)

    async def set_holdings(self, key: str, payload: dict[str, Any], ttl_seconds: int) -> None:
        del ttl_seconds
        self.holdings[key] = payload

    async def affairs_age_seconds(self) -> int | None:
        if self._affairs_at is None:
            return None
        return int((datetime.now(timezone.utc) - self._affairs_at).total_seconds())


class MemoryTokenStore:
    def __init__(self, token: str | None = None) -> None:
        self.token = token

    async def get_upstox_token(self) -> str | None:
        return self.token


class MemoryFeedLock:
    def __init__(self) -> None:
        self.held = False

    async def acquire(self) -> bool:
        if self.held:
            return False
        self.held = True
        return True

    async def release(self) -> None:
        self.held = False


class MemoryFeedStateStore:
    def __init__(self) -> None:
        self.state = FeedState()

    async def get(self) -> FeedState:
        return self.state

    async def set(self, state: FeedState) -> None:
        self.state = state


class MemoryResolver:
    def __init__(self, instruments: tuple[Instrument, ...] | None = None) -> None:
        self.instruments = instruments or (
            Instrument(
                symbol="RELIANCE",
                isin="INE002A01018",
                instrument_key="NSE_EQ|INE002A01018",
            ),
        )

    async def resolve_nifty50(self) -> tuple[Instrument, ...]:
        return self.instruments


class CountingFetch:
    def __init__(self, body: dict[str, Any]) -> None:
        self.body = body
        self.calls = 0

    async def fetch_page(self, instrument_keys: tuple[str, ...], page_number: int = 1) -> dict[str, Any]:
        del instrument_keys, page_number
        self.calls += 1
        return self.body


def holdings_cache_key(symbols: tuple[str, ...]) -> str:
    unique = ",".join(sorted({_norm(s) for s in symbols if s.strip()}))
    digest = hashlib.sha1(unique.encode("utf-8")).hexdigest()
    return f"news:holdings:v1:{digest}"


AFFAIRS_KEY = "news:affairs:v1"
TOKEN_KEY = "market_data:upstox:access_token"
LOCK_KEY = "news:feed:lock"


def normalize_symbols(symbols: list[str], cap: int | None = None) -> tuple[str, ...]:
    limit = cap if cap is not None else settings.symbols_cap
    seen: list[str] = []
    for symbol in symbols:
        value = _norm(symbol)
        if not value or value in seen:
            continue
        seen.append(value)
        if len(seen) >= limit:
            break
    return tuple(seen)
