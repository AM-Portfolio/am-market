import asyncio

from am_news.application.replay_raw import ReplayRaw
from am_news.application.start_feed import StartFeed
from am_news.domain.entities import ProcessStatus, RawBatch
from tests.conftest import fixture_body, make_container


def test_replay_uses_stored_body_zero_http():
    fetch = make_container().fetch
    container = make_container(fetch=fetch)
    body = fixture_body()
    container.raw.put(
        RawBatch(
            raw_id="r1",
            instrument_keys=("NSE_EQ|INE002A01018",),
            body=body,
            process_status=ProcessStatus.pending,
        )
    )
    before = fetch.calls
    result = asyncio.run(ReplayRaw(container).execute())
    assert fetch.calls == before
    assert result.processed == 1


def test_feed_lock_409(user_client, container):
    container.lock.held = True
    response = user_client.post("/v1/admin/feed/start")
    assert response.status_code == 409
    assert response.json()["error_code"] == "CONFLICT"


def test_token_missing_no_vendor_http():
    fetch = make_container().fetch
    container = make_container(token=None, fetch=fetch)
    result = asyncio.run(StartFeed(container).execute(http_start=True))
    assert result.status.value == "token_missing"
    assert fetch.calls == 0


def test_start_feed_persists_and_processes(user_client, container):
    response = user_client.post("/v1/admin/feed/start")
    assert response.status_code == 202
    assert container.fetch.calls >= 1
    rows = list(container.raw._rows.values())
    assert rows
    assert rows[0].body
    assert rows[0].process_status.value in {"processed", "failed"}
    assert container.articles._articles
