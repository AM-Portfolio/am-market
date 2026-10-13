from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class ProcessStatus(str, Enum):
    pending = "pending"
    processing = "processing"
    failed = "failed"
    processed = "processed"
    dead = "dead"


class FeedStatus(str, Enum):
    idle = "idle"
    running = "running"
    token_missing = "token_missing"


class Instrument(BaseModel):
    symbol: str
    isin: str = ""
    instrument_key: str


class NewsArticle(BaseModel):
    provider: str = "upstox"
    article_uid: str
    heading: str
    summary: str = ""
    thumbnail: str | None = None
    article_link: str
    published_at: datetime
    symbols: tuple[Instrument, ...] = ()


class RawBatch(BaseModel):
    raw_id: str
    instrument_keys: tuple[str, ...]
    body: dict[str, Any]
    process_status: ProcessStatus = ProcessStatus.pending
    attempts: int = 0
    error: str | None = None
    created_at: datetime | None = None


class FeedState(BaseModel):
    status: FeedStatus = FeedStatus.idle
    last_sync_at: datetime | None = None
    affairs_age_seconds: int | None = None
    dead_count: int = 0
    pages_fetched: int = 0
    message: str | None = None


class Principal(BaseModel):
    subject: str
    roles: tuple[str, ...] = ()
    access_token: str = ""
