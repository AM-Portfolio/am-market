from __future__ import annotations

from dataclasses import dataclass

from am_news.adapters.market_data import MarketDataInstrumentResolver
from am_news.adapters.memory import (
    MemoryArticleStore,
    MemoryFeedLock,
    MemoryFeedStateStore,
    MemoryRawStore,
    MemorySnapshotCache,
    MemoryTokenStore,
)
from am_news.adapters.mongo import MongoStores
from am_news.adapters.parse import UpstoxNewsParse
from am_news.adapters.redis_store import RedisStores
from am_news.adapters.upstox import UpstoxNewsAdapter
from am_news.domain.ports import (
    ArticleStore,
    FeedLock,
    FeedStateStore,
    InstrumentResolver,
    NewsFetchPort,
    NewsParsePort,
    RawBatchStore,
    SnapshotCache,
    TokenStore,
)
from am_news.settings import settings


@dataclass
class Container:
    articles: ArticleStore
    raw: RawBatchStore
    snapshots: SnapshotCache
    tokens: TokenStore
    lock: FeedLock
    fetch: NewsFetchPort
    parse: NewsParsePort
    resolver: InstrumentResolver
    feed_state: FeedStateStore


class MongoFeedState:
    def __init__(self, stores: MongoStores) -> None:
        self._stores = stores

    async def get(self):
        return await self._stores.get_state()

    async def set(self, state) -> None:
        await self._stores.set_state(state)


def build_container() -> Container:
    parse = UpstoxNewsParse()
    if settings.mongo_connection:
        mongo = MongoStores(settings.mongo_connection, settings.mongo_db)
        articles = mongo
        raw = mongo
        feed_state = MongoFeedState(mongo)
    else:
        mongo = None
        articles = MemoryArticleStore()
        raw = MemoryRawStore()
        feed_state = MemoryFeedStateStore()
    if settings.redis_hostname:
        redis = RedisStores(settings.redis_hostname, settings.redis_port, settings.redis_password)
        snapshots = redis
        tokens = redis
        lock = redis
    else:
        snapshots = MemorySnapshotCache()
        tokens = MemoryTokenStore()
        lock = MemoryFeedLock()
    fetch = UpstoxNewsAdapter(tokens)
    resolver = MarketDataInstrumentResolver()
    return Container(
        articles=articles,
        raw=raw,
        snapshots=snapshots,
        tokens=tokens,
        lock=lock,
        fetch=fetch,
        parse=parse,
        resolver=resolver,
        feed_state=feed_state,
    )
