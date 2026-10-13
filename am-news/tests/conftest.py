from __future__ import annotations

import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from am_news.adapters.memory import (
    CountingFetch,
    MemoryArticleStore,
    MemoryFeedLock,
    MemoryFeedStateStore,
    MemoryRawStore,
    MemoryResolver,
    MemorySnapshotCache,
    MemoryTokenStore,
)
from am_news.adapters.parse import UpstoxNewsParse
from am_news.api import deps
from am_news.container import Container
from am_news.domain.entities import Principal
from am_news.main import app

FIXTURE = Path(__file__).parent / "fixtures" / "upstox_news_page.json"


def fixture_body() -> dict:
    return json.loads(FIXTURE.read_text(encoding="utf-8"))


def make_container(*, token: str | None = "tok", fetch=None) -> Container:
    tokens = MemoryTokenStore(token)
    return Container(
        articles=MemoryArticleStore(),
        raw=MemoryRawStore(),
        snapshots=MemorySnapshotCache(),
        tokens=tokens,
        lock=MemoryFeedLock(),
        fetch=fetch or CountingFetch(fixture_body()),
        parse=UpstoxNewsParse(),
        resolver=MemoryResolver(),
        feed_state=MemoryFeedStateStore(),
    )


@pytest.fixture
def container() -> Container:
    return make_container()


@pytest.fixture
def user_client(container: Container) -> TestClient:
    app.state.container = container
    app.dependency_overrides[deps.require_user] = lambda: Principal(subject="u1", roles=("user",))
    app.dependency_overrides[deps.require_admin] = lambda: Principal(
        subject="admin", roles=("admin",)
    )
    with TestClient(app) as client:
        app.state.container = container
        yield client
    app.dependency_overrides.clear()


@pytest.fixture
def anon_client(container: Container) -> TestClient:
    app.state.container = container
    app.dependency_overrides.clear()
    with TestClient(app) as client:
        app.state.container = container
        yield client
    app.dependency_overrides.clear()
