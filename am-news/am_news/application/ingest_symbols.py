from __future__ import annotations

from fastapi import HTTPException

from am_news.adapters.memory import normalize_symbols
from am_news.application.process_raw import ProcessRaw
from am_news.domain.entities import FeedStatus, ProcessStatus, RawBatch
from am_news.domain.universe import NIFTY50_SYMBOLS, chunked
from am_news.schemas.news import AcceptedFeedResponse, ErrorEnvelope
from am_news.settings import settings


class IngestSymbols:
    """Fetch Upstox news for arbitrary symbols without rewriting NIFTY affairs."""

    def __init__(self, container) -> None:
        self._c = container

    async def execute(self, symbols: list[str], *, http_start: bool = False) -> AcceptedFeedResponse:
        wanted = normalize_symbols(symbols, settings.symbols_cap)
        if not wanted:
            return AcceptedFeedResponse(status=FeedStatus.idle, message="no_symbols")
        acquired = await self._c.symbols_lock.acquire()
        if not acquired:
            if http_start:
                raise HTTPException(
                    status_code=409,
                    detail=ErrorEnvelope(
                        error_code="CONFLICT",
                        message="Symbol feed is already running",
                    ).model_dump(),
                )
            return AcceptedFeedResponse(status=FeedStatus.running, message="skipped_lock")
        try:
            token = await self._c.tokens.get_upstox_token()
            if not token:
                return AcceptedFeedResponse(
                    status=FeedStatus.token_missing, message="token_missing"
                )
            instruments = await self._c.resolver.resolve_symbols(list(wanted))
            if not instruments:
                return AcceptedFeedResponse(status=FeedStatus.idle, message="resolve_empty")
            process = ProcessRaw(
                self._c.parse, self._c.articles, self._c.snapshots, NIFTY50_SYMBOLS
            )
            pages = 0
            for batch in chunked(instruments, settings.instrument_batch_size):
                keys = tuple(item.instrument_key for item in batch)
                body = await self._c.fetch.fetch_page(keys)
                raw_id = await self._c.raw.insert(
                    RawBatch(
                        raw_id="",
                        instrument_keys=keys,
                        body=body,
                        process_status=ProcessStatus.pending,
                    )
                )
                try:
                    await process.execute(body, batch, warm_affairs=False)
                    await self._c.raw.mark(raw_id, ProcessStatus.processed)
                except Exception as exc:
                    await self._c.raw.mark(raw_id, ProcessStatus.failed, str(exc))
                pages += 1
            await self._c.snapshots.clear_holdings()
            return AcceptedFeedResponse(
                status=FeedStatus.idle, message=f"completed:{pages}"
            )
        finally:
            await self._c.symbols_lock.release()
