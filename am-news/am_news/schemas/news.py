from datetime import datetime, timezone
from typing import Any

from pydantic import BaseModel, Field

from am_news.domain.entities import FeedStatus, ProcessStatus


class ErrorEnvelope(BaseModel):
    error_code: str = Field(examples=["UNAUTHORIZED"])
    message: str
    details: dict[str, Any] = Field(default_factory=dict)


class NewsCard(BaseModel):
    heading: str = Field(examples=["Weekly Wrap: NIFTY50, SENSEX drop 1% this week"])
    summary: str = ""
    thumbnail: str | None = None
    article_link: str = Field(
        examples=["https://upstox.com/news/market-news/stocks/example/article-199849/"]
    )
    published_at: datetime
    symbols: list[str] = Field(default_factory=list, examples=[["RELIANCE"]])


class CurrentAffairsResponse(BaseModel):
    items: list[NewsCard] = Field(default_factory=list)


class InsightRequest(BaseModel):
    symbols: list[str] = Field(default_factory=list, examples=[["RELIANCE", "TCS"]])


class InsightResponse(BaseModel):
    current_affairs: list[NewsCard] = Field(default_factory=list)
    holdings: list[NewsCard] = Field(default_factory=list)


class HoldingsNewsResponse(BaseModel):
    items: list[NewsCard] = Field(default_factory=list)


class FeedStatusResponse(BaseModel):
    status: FeedStatus
    last_sync_at: datetime | None = None
    affairs_age_seconds: int | None = None
    dead_count: int = 0
    pages_fetched: int = 0
    message: str | None = None
    generated_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class RawBatchSummary(BaseModel):
    raw_id: str
    process_status: ProcessStatus
    instrument_keys: list[str]
    attempts: int
    error: str | None = None
    has_body: bool = True


class RawListResponse(BaseModel):
    items: list[RawBatchSummary] = Field(default_factory=list)


class ReplayResponse(BaseModel):
    processed: int
    failed: int
    skipped: int = 0


class AcceptedFeedResponse(BaseModel):
    status: FeedStatus
    message: str
