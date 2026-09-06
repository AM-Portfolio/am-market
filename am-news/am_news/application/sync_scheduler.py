from datetime import datetime
from zoneinfo import ZoneInfo

from am_news.application.start_feed import StartFeed
from am_news.settings import settings

IST = ZoneInfo("Asia/Kolkata")


def should_sync(now: datetime, last_slot: str | None) -> str | None:
    local = now.astimezone(IST)
    hhmm = local.strftime("%H:%M")
    if hhmm in {"08:30", "18:00"} and last_slot != hhmm:
        return hhmm
    if 9 <= local.hour < 16 and local.minute in {0, 30}:
        slot = f"{local.hour:02d}:{local.minute:02d}"
        if last_slot != slot:
            return slot
    return None


class SyncScheduler:
    def __init__(self, container) -> None:
        self._start = StartFeed(container)
        self._last_slot: str | None = None

    async def tick(self, now: datetime | None = None) -> bool:
        if not settings.news_sync_enabled:
            return False
        moment = now or datetime.now(IST)
        slot = should_sync(moment, self._last_slot)
        if slot is None:
            return False
        self._last_slot = slot
        await self._start.execute(http_start=False)
        return True
