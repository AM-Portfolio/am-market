from typing import Any, Protocol

from am_news.domain.entities import FeedState, Instrument, NewsArticle, ProcessStatus, RawBatch


class NewsFetchPort(Protocol):
    async def fetch_page(self, instrument_keys: tuple[str, ...], page_number: int = 1) -> dict[str, Any]:
        ...


class NewsParsePort(Protocol):
    def parse(self, body: dict[str, Any], instruments: tuple[Instrument, ...]) -> tuple[NewsArticle, ...]:
        ...


class ArticleStore(Protocol):
    async def upsert_articles(self, articles: tuple[NewsArticle, ...]) -> None:
        ...

    async def latest_for_universe(self, symbols: tuple[str, ...], limit: int) -> tuple[NewsArticle, ...]:
        ...

    async def latest_for_symbols(self, symbols: tuple[str, ...], limit: int) -> tuple[NewsArticle, ...]:
        ...


class RawBatchStore(Protocol):
    async def insert(self, batch: RawBatch) -> str:
        ...

    async def get(self, raw_id: str) -> RawBatch | None:
        ...

    async def list_recent(self, limit: int = 50) -> tuple[RawBatch, ...]:
        ...

    async def claim_next(self, statuses: tuple[ProcessStatus, ...]) -> RawBatch | None:
        ...

    async def mark(self, raw_id: str, status: ProcessStatus, error: str | None = None) -> None:
        ...

    async def dead_count(self) -> int:
        ...


class SnapshotCache(Protocol):
    async def get_affairs(self) -> dict[str, Any] | None:
        ...

    async def set_affairs(self, payload: dict[str, Any], ttl_seconds: int) -> None:
        ...

    async def get_holdings(self, key: str) -> dict[str, Any] | None:
        ...

    async def set_holdings(self, key: str, payload: dict[str, Any], ttl_seconds: int) -> None:
        ...

    async def affairs_age_seconds(self) -> int | None:
        ...


class TokenStore(Protocol):
    async def get_upstox_token(self) -> str | None:
        ...


class FeedLock(Protocol):
    async def acquire(self) -> bool:
        ...

    async def release(self) -> None:
        ...


class InstrumentResolver(Protocol):
    async def resolve_nifty50(self) -> tuple[Instrument, ...]:
        ...


class FeedStateStore(Protocol):
    async def get(self) -> FeedState:
        ...

    async def set(self, state: FeedState) -> None:
        ...
