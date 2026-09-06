from datetime import datetime
from zoneinfo import ZoneInfo

from am_news.application.sync_scheduler import should_sync

IST = ZoneInfo("Asia/Kolkata")


def test_sync_slots():
    morning = datetime(2026, 9, 7, 8, 30, tzinfo=IST)
    assert should_sync(morning, None) == "08:30"
    assert should_sync(morning, "08:30") is None
    midday = datetime(2026, 9, 7, 10, 0, tzinfo=IST)
    assert should_sync(midday, None) == "10:00"
    evening = datetime(2026, 9, 7, 18, 0, tzinfo=IST)
    assert should_sync(evening, None) == "18:00"
    night = datetime(2026, 9, 7, 21, 0, tzinfo=IST)
    assert should_sync(night, None) is None
