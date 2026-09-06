from datetime import datetime, timezone

from fastapi import HTTPException

from am_news.application.process_raw import ProcessRaw
from am_news.domain.entities import FeedState, FeedStatus, ProcessStatus, RawBatch
from am_news.domain.universe import NIFTY50_SYMBOLS, chunked
from am_news.schemas.news import AcceptedFeedResponse, ErrorEnvelope
from am_news.settings import settings


class StartFeed:
    def __init__(self, container) -> None:
        self._c = container
        self._process = ProcessRaw(
            container.parse, container.articles, container.snapshots, NIFTY50_SYMBOLS
        )

    async def execute(self, http_start: bool) -> AcceptedFeedResponse:
        acquired = await self._c.lock.acquire()
        if not acquired:
            if http_start:
                raise HTTPException(
                    status_code=409,
                    detail=ErrorEnvelope(
                        error_code="CONFLICT",
                        message="Feed is already running",
                    ).model_dump(),
                )
            return AcceptedFeedResponse(status=FeedStatus.running, message="skipped_lock")
        try:
            token = await self._c.tokens.get_upstox_token()
            if not token:
                state = FeedState(status=FeedStatus.token_missing, message="token_missing")
                await self._c.feed_state.set(state)
                return AcceptedFeedResponse(status=FeedStatus.token_missing, message="token_missing")
            instruments = await self._c.resolver.resolve_nifty50()
            self._process = ProcessRaw(
                self._c.parse, self._c.articles, self._c.snapshots, NIFTY50_SYMBOLS
            )
            await self._c.feed_state.set(FeedState(status=FeedStatus.running))
            pages = 0
            for batch in chunked(instruments, settings.instrument_batch_size):
                keys = tuple(item.instrument_key for item in batch)
                body = await self._c.fetch.fetch_page(keys)
                raw_id = await self._c.raw.insert(
                    RawBatch(raw_id="", instrument_keys=keys, body=body, process_status=ProcessStatus.pending)
                )
                try:
                    await self._process.execute(body, batch)
                    await self._c.raw.mark(raw_id, ProcessStatus.processed)
                except Exception as exc:
                    await self._c.raw.mark(raw_id, ProcessStatus.failed, str(exc))
                pages += 1
            dead = await self._c.raw.dead_count()
            await self._c.feed_state.set(
                FeedState(
                    status=FeedStatus.idle,
                    last_sync_at=datetime.now(timezone.utc),
                    pages_fetched=pages,
                    dead_count=dead,
                )
            )
            return AcceptedFeedResponse(status=FeedStatus.idle, message="completed")
        finally:
            await self._c.lock.release()
