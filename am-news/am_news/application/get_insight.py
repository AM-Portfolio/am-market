from __future__ import annotations

import asyncio
import logging

from am_news.adapters.memory import holdings_cache_key, normalize_symbols
from am_news.application.get_current_affairs import GetCurrentAffairs
from am_news.schemas.mappers import article_to_card, cards_from_payload, cards_payload
from am_news.schemas.news import HoldingsNewsResponse, InsightResponse
from am_news.settings import settings

logger = logging.getLogger(__name__)


class GetInsight:
    def __init__(
        self,
        snapshots,
        articles,
        universe_symbols: tuple[str, ...],
        container=None,
    ) -> None:
        self._snapshots = snapshots
        self._articles = articles
        self._affairs = GetCurrentAffairs(snapshots, articles, universe_symbols)
        self._container = container

    async def execute(self, symbols: list[str], include_affairs: bool) -> InsightResponse:
        wanted = normalize_symbols(symbols, settings.symbols_cap)
        holdings = await self._holdings(wanted)
        covered = {
            symbol.strip().upper()
            for card in holdings
            for symbol in card.symbols
            if symbol and symbol.strip()
        }
        missing = [symbol for symbol in wanted if symbol not in covered]
        pending = False
        if missing and settings.news_sync_enabled and self._container is not None:
            pending = self._enqueue_ingest(missing)
        if not include_affairs:
            return InsightResponse(
                current_affairs=[],
                holdings=holdings,
                pending_ingest=pending,
                missing_symbols=list(missing) if pending else [],
            )
        affairs = await self._affairs.execute()
        return InsightResponse(
            current_affairs=affairs.items,
            holdings=holdings,
            pending_ingest=pending,
            missing_symbols=list(missing) if pending else [],
        )

    def _enqueue_ingest(self, missing: list[str]) -> bool:
        try:
            from am_news.application.ingest_symbols import IngestSymbols

            async def _run() -> None:
                try:
                    await IngestSymbols(self._container).execute(missing, http_start=False)
                except Exception:
                    logger.exception("background_symbol_ingest_failed")

            loop = asyncio.get_running_loop()
            loop.create_task(_run())
            return True
        except Exception:
            logger.exception("enqueue_symbol_ingest_failed")
            return False

    async def holdings_only(self, symbols: list[str]) -> HoldingsNewsResponse:
        wanted = normalize_symbols(symbols, settings.symbols_cap)
        return HoldingsNewsResponse(items=await self._holdings(wanted))

    async def _holdings(self, wanted: tuple[str, ...]) -> list:
        if not wanted:
            return []
        key = holdings_cache_key(wanted)
        cached = await self._snapshots.get_holdings(key)
        if cached:
            return cards_from_payload(cached)[: settings.holdings_limit]
        articles = await self._articles.latest_for_symbols(wanted, settings.holdings_limit)
        cards = [article_to_card(article) for article in articles]
        filtered = []
        for card in cards:
            narrowed = card.model_copy(
                update={"symbols": [symbol for symbol in card.symbols if symbol in wanted]}
            )
            if narrowed.symbols:
                filtered.append(narrowed)
        await self._snapshots.set_holdings(key, cards_payload(filtered), settings.holdings_ttl_seconds)
        return filtered[: settings.holdings_limit]
