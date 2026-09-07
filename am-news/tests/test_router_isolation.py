from pathlib import Path

from am_news.api.routes import admin_feed, admin_replay, current_affairs, holdings, insight


def test_user_routers_do_not_import_upstox_or_adapters():
    for module in (current_affairs, insight, holdings):
        source = Path(module.__file__).read_text(encoding="utf-8")
        assert "from am_news.adapters" not in source
        assert "import httpx" not in source
        assert "adapters.upstox" not in source


def test_admin_routers_stay_thin():
    for module in (admin_feed, admin_replay):
        source = Path(module.__file__).read_text(encoding="utf-8")
        assert "am_news.adapters.upstox" not in source
        assert "httpx" not in source
