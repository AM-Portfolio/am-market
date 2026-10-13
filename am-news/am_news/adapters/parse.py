from __future__ import annotations

import hashlib
from datetime import datetime, timezone
from typing import Any

from am_news.domain.entities import Instrument, NewsArticle


class UpstoxNewsParse:
    provider = "upstox"

    def parse(self, body: dict[str, Any], instruments: tuple[Instrument, ...]) -> tuple[NewsArticle, ...]:
        data = body.get("data")
        by_key = {item.instrument_key: item for item in instruments}
        collected: dict[str, NewsArticle] = {}
        if not isinstance(data, dict):
            return ()
        for instrument_key, articles in data.items():
            instrument = by_key.get(str(instrument_key))
            if instrument is None or not isinstance(articles, list):
                continue
            for raw in articles:
                if not isinstance(raw, dict):
                    continue
                article = _article_from_raw(raw, instrument)
                existing = collected.get(article.article_uid)
                if existing is None:
                    collected[article.article_uid] = article
                    continue
                merged = {item.instrument_key: item for item in existing.symbols}
                merged[instrument.instrument_key] = instrument
                collected[article.article_uid] = existing.model_copy(update={"symbols": tuple(merged.values())})
        return tuple(collected.values())


def _article_from_raw(raw: dict[str, Any], instrument: Instrument) -> NewsArticle:
    link = str(raw.get("article_link") or "")
    uid = str(raw.get("id") or raw.get("article_id") or "") or hashlib.sha256(link.encode("utf-8")).hexdigest()
    published_raw = raw.get("published_time") or raw.get("published_at") or 0
    if isinstance(published_raw, (int, float)):
        published = datetime.fromtimestamp(float(published_raw) / 1000.0, tz=timezone.utc)
    else:
        published = datetime.now(timezone.utc)
    return NewsArticle(
        provider="upstox",
        article_uid=uid,
        heading=str(raw.get("heading") or ""),
        summary=str(raw.get("summary") or ""),
        thumbnail=raw.get("thumbnail"),
        article_link=link,
        published_at=published,
        symbols=(instrument,),
    )
