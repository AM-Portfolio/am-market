import json
from pathlib import Path

from fastapi.testclient import TestClient

from am_news.main import app

OUT = Path(__file__).resolve().parents[1] / "postman" / "AM-News.postman_collection.json"


def test_openapi_covers_news_routes():
    with TestClient(app) as client:
        schema = client.get("/openapi.json").json()
    paths = schema["paths"]
    assert "/v1/current-affairs" in paths
    assert "/v1/insight" in paths
    assert "/v1/holdings" in paths
    assert "/v1/admin/feed/start" in paths


def test_committed_postman_collection_is_runnable():
    collection = json.loads(OUT.read_text(encoding="utf-8"))
    assert collection["info"]["name"] == "AM News"
    folders = {folder["name"] for folder in collection["item"]}
    assert {"Health", "User", "Admin"} <= folders
    insight = next(
        req
        for folder in collection["item"]
        if folder["name"] == "User"
        for req in folder["item"]
        if req["name"] == "Insight"
    )
    assert insight["request"]["method"] == "POST"
    assert "/v1/insight" in insight["request"]["url"]
    assert "symbols" in insight["request"]["body"]["raw"]
    keys = {var["key"] for var in collection["variable"]}
    assert {"baseUrl", "userJwt", "adminJwt"} <= keys
