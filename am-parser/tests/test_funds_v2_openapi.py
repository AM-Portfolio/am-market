"""v2 funds routes appear in OpenAPI; v1 etf paths remain."""
from fastapi.testclient import TestClient

from am_api.api import app

client = TestClient(app)


def test_openapi_includes_v2_funds_and_frozen_v1():
    response = client.get("/openapi.json")
    assert response.status_code == 200
    paths = response.json()["paths"]
    assert "/v1/etf/holdings" in paths
    assert "/v2/funds/holdings" in paths
    assert "/v2/funds/search" in paths
    assert "/v2/funds/holdings/bulk" in paths
    assert "/v2/funds/performance/batch" in paths
    assert "/v2/funds/performance/refresh" in paths
    schemas = response.json()["components"]["schemas"]
    assert "FundHoldingsLookupRequest" in schemas
    assert "FundHoldingsLookupResponse" in schemas
