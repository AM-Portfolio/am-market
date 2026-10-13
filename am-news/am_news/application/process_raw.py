from am_news.domain.entities import Instrument, NewsArticle
from am_news.schemas.mappers import article_to_card, cards_payload
from am_news.settings import settings


class ProcessRaw:
    def __init__(self, parse, articles, snapshots, universe_symbols: tuple[str, ...]) -> None:
        self._parse = parse
        self._articles = articles
        self._snapshots = snapshots
        self._universe = universe_symbols

    async def execute(self, body: dict, instruments: tuple[Instrument, ...]) -> tuple[NewsArticle, ...]:
        parsed = self._parse.parse(body, instruments)
        if parsed:
            await self._articles.upsert_articles(parsed)
            latest = await self._articles.latest_for_universe(self._universe, settings.affairs_limit)
            cards = [article_to_card(article) for article in latest]
            await self._snapshots.set_affairs(cards_payload(cards), settings.affairs_ttl_seconds)
        return parsed
