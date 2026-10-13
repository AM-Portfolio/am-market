from fastapi.testclient import TestClient

from am_news.main import app

NAMED = {
    "NewsCard",
    "InsightRequest",
    "InsightResponse",
    "CurrentAffairsResponse",
    "HoldingsNewsResponse",
    "ErrorEnvelope",
    "FeedStatusResponse",
    "ProcessStatus",
    "FeedStatus",
}

OPERATION_IDS = {
    "getCurrentAffairs",
    "postInsight",
    "getHoldings",
    "startFeed",
}


def test_openapi_named_schemas_and_operation_ids():
    with TestClient(app) as client:
        response = client.get("/openapi.json")
    assert response.status_code == 200
    schema = response.json()
    assert schema["info"]["title"] == "AM News"
    components = schema["components"]["schemas"]
    for name in NAMED:
        assert name in components
    paths = schema["paths"]
    for path in paths:
        assert "am-news" not in path
        assert "news-service" not in path
        assert not path.startswith("/v1/news/")
    found = set()
    for path_item in paths.values():
        for operation in path_item.values():
            if isinstance(operation, dict) and "operationId" in operation:
                found.add(operation["operationId"])
    assert OPERATION_IDS <= found
    insight = paths["/v1/insight"]["post"]
    schema_obj = insight["responses"]["200"]["content"]["application/json"]["schema"]
    ref = schema_obj.get("$ref", "")
    if not ref:
        for item in schema_obj.get("allOf") or schema_obj.get("anyOf") or []:
            if isinstance(item, dict) and item.get("$ref"):
                ref = item["$ref"]
                break
    assert ref.endswith("InsightResponse")
