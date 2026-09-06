from datetime import datetime, timezone
from typing import Any

from am_news.domain.entities import NewsArticle
from am_news.schemas.news import NewsCard


def article_to_card(article: NewsArticle) -> NewsCard:
    published = article.published_at
    if published.tzinfo is None:
        published = published.replace(tzinfo=timezone.utc)
    return NewsCard(
        heading=article.heading,
        summary=article.summary,
        thumbnail=article.thumbnail,
        article_link=article.article_link,
        published_at=published,
        symbols=sorted({item.symbol for item in article.symbols if item.symbol}),
    )


def cards_payload(cards: list[NewsCard]) -> dict[str, Any]:
    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "items": [card.model_dump(mode="json") for card in cards],
    }


def cards_from_payload(payload: dict[str, Any]) -> list[NewsCard]:
    items = payload.get("items") or []
    return [NewsCard.model_validate(item) for item in items if isinstance(item, dict)]
