from datetime import datetime, timezone

from am_news.application.process_raw import ProcessRaw
from am_news.domain.entities import Instrument, NewsArticle
from am_news.domain.universe import NIFTY50_SYMBOLS


def test_empty_insight_is_200(user_client):
    response = user_client.post("/v1/insight", json={"symbols": []})
    assert response.status_code == 200
    body = response.json()
    assert body["current_affairs"] == []
    assert body["holdings"] == []
    assert body.get("pending_ingest") is False
    assert body.get("missing_symbols") == []
    assert response.headers["cache-control"] == "private, max-age=30"


def test_empty_current_affairs_is_200(user_client):
    response = user_client.get("/v1/current-affairs")
    assert response.status_code == 200
    assert response.json()["items"] == []


def test_user_insight_never_calls_vendor(user_client, container):
    user_client.post("/v1/insight", json={"symbols": ["RELIANCE"]})
    user_client.get("/v1/current-affairs")
    user_client.get("/v1/holdings", params=[("symbols", "RELIANCE")])
    assert container.fetch.calls == 0


def test_holdings_filter_and_cap(user_client, container):
    now = datetime.now(timezone.utc)
    reliance = Instrument(symbol="RELIANCE", instrument_key="NSE_EQ|INE002A01018")
    tcs = Instrument(symbol="TCS", instrument_key="NSE_EQ|INE467B01029")
    extra = Instrument(symbol="INFY", instrument_key="NSE_EQ|INE009A01021")
    articles = (
        NewsArticle(
            article_uid="a1",
            heading="Reliance story",
            article_link="https://example.com/a1",
            published_at=now,
            symbols=(reliance,),
        ),
        NewsArticle(
            article_uid="a2",
            heading="TCS story",
            article_link="https://example.com/a2",
            published_at=now,
            symbols=(tcs,),
        ),
        NewsArticle(
            article_uid="a3",
            heading="Infosys story",
            article_link="https://example.com/a3",
            published_at=now,
            symbols=(extra,),
        ),
    )
    container.articles.put(articles)
    response = user_client.post("/v1/insight", json={"symbols": ["RELIANCE", "tcs"]})
    assert response.status_code == 200
    holdings = response.json()["holdings"]
    headings = {item["heading"] for item in holdings}
    assert headings == {"Reliance story", "TCS story"}
    for item in holdings:
        assert set(item["symbols"]) <= {"RELIANCE", "TCS"}


def test_two_keys_one_article(container):
    body = {
        "status": "success",
        "data": {
            "NSE_EQ|AAA": [{"heading": "Same", "article_link": "https://x/1", "published_time": 1, "summary": "", "thumbnail": None}],
            "NSE_EQ|BBB": [{"heading": "Same", "article_link": "https://x/1", "published_time": 1, "summary": "", "thumbnail": None}],
        },
    }
    instruments = (
        Instrument(symbol="RELIANCE", instrument_key="NSE_EQ|AAA"),
        Instrument(symbol="TCS", instrument_key="NSE_EQ|BBB"),
    )
    parsed = container.parse.parse(body, instruments)
    assert len(parsed) == 1
    assert {item.symbol for item in parsed[0].symbols} == {"RELIANCE", "TCS"}


def test_unknown_instrument_key_skipped(container):
    body = {
        "status": "success",
        "data": {
            "NSE_EQ|UNKNOWN": [
                {"heading": "X", "article_link": "https://x/u", "published_time": 1, "summary": "", "thumbnail": None}
            ]
        },
    }
    parsed = container.parse.parse(
        body, (Instrument(symbol="RELIANCE", instrument_key="NSE_EQ|INE002A01018"),)
    )
    assert parsed == ()


def test_process_refreshes_affairs(container):
    import asyncio

    body = {
        "status": "success",
        "data": {
            "NSE_EQ|INE002A01018": [
                {
                    "heading": "Nifty wrap",
                    "article_link": "https://x/nifty",
                    "published_time": 1788585096693,
                    "summary": "s",
                    "thumbnail": None,
                }
            ]
        },
    }
    instruments = (Instrument(symbol="RELIANCE", isin="INE002A01018", instrument_key="NSE_EQ|INE002A01018"),)
    asyncio.run(
        ProcessRaw(container.parse, container.articles, container.snapshots, NIFTY50_SYMBOLS).execute(
            body, instruments
        )
    )
    cached = asyncio.run(container.snapshots.get_affairs())
    assert cached is not None
    assert cached["items"][0]["heading"] == "Nifty wrap"


def test_process_warm_affairs_false_skips_snapshot(container):
    import asyncio

    body = {
        "status": "success",
        "data": {
            "NSE_EQ|INE002A01018": [
                {
                    "heading": "Holdings only",
                    "article_link": "https://x/h",
                    "published_time": 1788585096693,
                    "summary": "s",
                    "thumbnail": None,
                }
            ]
        },
    }
    instruments = (Instrument(symbol="RELIANCE", isin="INE002A01018", instrument_key="NSE_EQ|INE002A01018"),)
    asyncio.run(
        ProcessRaw(container.parse, container.articles, container.snapshots, NIFTY50_SYMBOLS).execute(
            body, instruments, warm_affairs=False
        )
    )
    assert asyncio.run(container.snapshots.get_affairs()) is None
    assert container.articles._articles


def test_pending_ingest_when_sync_enabled(user_client, container, monkeypatch):
    monkeypatch.setattr("am_news.application.get_insight.settings.news_sync_enabled", True)
    response = user_client.post("/v1/insight", json={"symbols": ["IDEA"]})
    assert response.status_code == 200
    body = response.json()
    assert body["holdings"] == []
    assert body["pending_ingest"] is True
    assert "IDEA" in body["missing_symbols"]


def test_admin_ingest_symbols_no_affairs_warm(user_client, container):
    from am_news.domain.entities import Instrument
    from am_news.adapters.memory import MemoryResolver

    container.resolver = MemoryResolver(
        (
            Instrument(symbol="IDEA", isin="INE669E01016", instrument_key="NSE_EQ|INE669E01016"),
        )
    )
    # Point fixture key at IDEA for parse match — CountingFetch returns RELIANCE fixture;
    # use a custom fetch body with IDEA key.
    from am_news.adapters.memory import CountingFetch

    body = {
        "status": "success",
        "data": {
            "NSE_EQ|INE669E01016": [
                {
                    "heading": "Idea story",
                    "article_link": "https://example.com/idea",
                    "published_time": 1788585096693,
                    "summary": "s",
                    "thumbnail": None,
                }
            ]
        },
    }
    container.fetch = CountingFetch(body)
    response = user_client.post("/v1/admin/feed/symbols", json={"symbols": ["IDEA"]})
    assert response.status_code == 202
    assert response.json()["message"].startswith("completed")
    assert container.snapshots.affairs is None
    assert container.articles._articles
    insight = user_client.post("/v1/insight", json={"symbols": ["IDEA"]})
    assert insight.status_code == 200
    assert any(item["heading"] == "Idea story" for item in insight.json()["holdings"])
