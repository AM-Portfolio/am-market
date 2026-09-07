from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from am_news.domain.entities import FeedState, Instrument, NewsArticle, ProcessStatus, RawBatch
from am_news.settings import settings

try:
    from motor.motor_asyncio import AsyncIOMotorClient
except ImportError:  # pragma: no cover
    AsyncIOMotorClient = None


def _norm(symbol: str) -> str:
    return symbol.strip().upper()


class MongoStores:
    def __init__(self, uri: str, db_name: str) -> None:
        if AsyncIOMotorClient is None:
            raise RuntimeError("motor is required for MongoStores")
        self._client = AsyncIOMotorClient(uri)
        self._db = self._client[db_name]
        self.articles = self._db["news_articles"]
        self.symbols = self._db["news_article_symbols"]
        self.raw = self._db["news_raw_batches"]
        self.feed = self._db["news_feed_state"]

    async def ensure_indexes(self) -> None:
        await self.articles.create_index([("provider", 1), ("article_uid", 1)], unique=True)
        await self.symbols.create_index([("article_id", 1), ("symbol", 1), ("instrument_key", 1)], unique=True)
        await self.raw.create_index("created_at", expireAfterSeconds=settings.raw_ttl_days * 86400)

    async def upsert_articles(self, articles: tuple[NewsArticle, ...]) -> None:
        for article in articles:
            doc = {
                "provider": article.provider,
                "article_uid": article.article_uid,
                "heading": article.heading,
                "summary": article.summary,
                "thumbnail": article.thumbnail,
                "article_link": article.article_link,
                "published_at": article.published_at,
            }
            result = await self.articles.update_one(
                {"provider": article.provider, "article_uid": article.article_uid},
                {"$set": doc},
                upsert=True,
            )
            article_id = result.upserted_id
            if article_id is None:
                existing = await self.articles.find_one(
                    {"provider": article.provider, "article_uid": article.article_uid}
                )
                article_id = existing["_id"] if existing else None
            if article_id is None:
                continue
            for item in article.symbols:
                await self.symbols.update_one(
                    {
                        "article_id": article_id,
                        "symbol": item.symbol,
                        "instrument_key": item.instrument_key,
                    },
                    {
                        "$set": {
                            "article_id": article_id,
                            "symbol": item.symbol,
                            "isin": item.isin,
                            "instrument_key": item.instrument_key,
                        }
                    },
                    upsert=True,
                )

    async def latest_for_universe(self, symbols: tuple[str, ...], limit: int) -> tuple[NewsArticle, ...]:
        return await self._latest(symbols, limit)

    async def latest_for_symbols(self, symbols: tuple[str, ...], limit: int) -> tuple[NewsArticle, ...]:
        return await self._latest(symbols, limit)

    async def _latest(self, symbols: tuple[str, ...], limit: int) -> tuple[NewsArticle, ...]:
        wanted = [_norm(s) for s in symbols]
        if not wanted:
            return ()
        cursor = self.symbols.find({"symbol": {"$in": wanted}})
        article_ids = {row["article_id"] async for row in cursor}
        if not article_ids:
            return ()
        docs = (
            await self.articles.find({"_id": {"$in": list(article_ids)}})
            .sort("published_at", -1)
            .limit(limit)
            .to_list(length=limit)
        )
        out: list[NewsArticle] = []
        for doc in docs:
            related = [row async for row in self.symbols.find({"article_id": doc["_id"]})]
            out.append(
                NewsArticle(
                    provider=doc.get("provider", "upstox"),
                    article_uid=doc["article_uid"],
                    heading=doc.get("heading", ""),
                    summary=doc.get("summary", ""),
                    thumbnail=doc.get("thumbnail"),
                    article_link=doc.get("article_link", ""),
                    published_at=doc.get("published_at") or datetime.now(timezone.utc),
                    symbols=tuple(
                        Instrument(
                            symbol=row.get("symbol", ""),
                            isin=row.get("isin", ""),
                            instrument_key=row.get("instrument_key", ""),
                        )
                        for row in related
                    ),
                )
            )
        return tuple(out)

    async def insert(self, batch: RawBatch) -> str:
        raw_id = batch.raw_id or uuid4().hex
        await self.raw.insert_one(
            {
                "raw_id": raw_id,
                "instrument_keys": list(batch.instrument_keys),
                "body": batch.body,
                "process_status": batch.process_status.value,
                "attempts": batch.attempts,
                "error": batch.error,
                "created_at": batch.created_at or datetime.now(timezone.utc),
            }
        )
        return raw_id

    async def get(self, raw_id: str) -> RawBatch | None:
        doc = await self.raw.find_one({"raw_id": raw_id})
        return self._raw_from_doc(doc) if doc else None

    async def list_recent(self, limit: int = 50) -> tuple[RawBatch, ...]:
        docs = await self.raw.find().sort("created_at", -1).limit(limit).to_list(length=limit)
        return tuple(self._raw_from_doc(doc) for doc in docs if doc)

    async def claim_next(self, statuses: tuple[ProcessStatus, ...]) -> RawBatch | None:
        doc = await self.raw.find_one_and_update(
            {"process_status": {"$in": [status.value for status in statuses]}},
            {"$set": {"process_status": ProcessStatus.processing.value}, "$inc": {"attempts": 1}},
        )
        return self._raw_from_doc(doc) if doc else None

    async def mark(self, raw_id: str, status: ProcessStatus, error: str | None = None) -> None:
        await self.raw.update_one(
            {"raw_id": raw_id},
            {"$set": {"process_status": status.value, "error": error}},
        )

    async def dead_count(self) -> int:
        return await self.raw.count_documents({"process_status": ProcessStatus.dead.value})

    async def get_state(self) -> FeedState:
        doc = await self.feed.find_one({"_id": "v1"})
        if not doc:
            return FeedState()
        return FeedState.model_validate({k: v for k, v in doc.items() if k != "_id"})

    async def set_state(self, state: FeedState) -> None:
        await self.feed.update_one({"_id": "v1"}, {"$set": state.model_dump(mode="json")}, upsert=True)

    def _raw_from_doc(self, doc: dict[str, Any]) -> RawBatch:
        return RawBatch(
            raw_id=doc["raw_id"],
            instrument_keys=tuple(doc.get("instrument_keys") or []),
            body=doc.get("body") or {},
            process_status=ProcessStatus(doc.get("process_status", "pending")),
            attempts=int(doc.get("attempts") or 0),
            error=doc.get("error"),
            created_at=doc.get("created_at"),
        )
