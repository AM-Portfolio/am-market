from am_news.schemas.mappers import article_to_card, cards_from_payload, cards_payload
from am_news.schemas.news import CurrentAffairsResponse
from am_news.settings import settings


class GetCurrentAffairs:
    def __init__(self, snapshots, articles, universe_symbols: tuple[str, ...]) -> None:
        self._snapshots = snapshots
        self._articles = articles
        self._universe = universe_symbols

    async def execute(self) -> CurrentAffairsResponse:
        cached = await self._snapshots.get_affairs()
        if cached:
            return CurrentAffairsResponse(items=cards_from_payload(cached)[: settings.affairs_limit])
        articles = await self._articles.latest_for_universe(self._universe, settings.affairs_limit)
        cards = [article_to_card(article) for article in articles]
        await self._snapshots.set_affairs(cards_payload(cards), settings.affairs_ttl_seconds)
        return CurrentAffairsResponse(items=cards)
