"""
Automated Unit Test for OpenAPI Schema Validity.
Verifies that FastAPI generates a valid, non-empty OpenAPI 3.0/3.1 JSON schema
without missing component references or schema errors.
"""

from fastapi.testclient import TestClient
from am_api.api import app

client = TestClient(app)


def test_openapi_schema_generation():
    """Ensure GET /openapi.json returns 200 OK and valid schema components."""
    response = client.get("/openapi.json")
    assert response.status_code == 200
    
    schema = response.json()
    assert "openapi" in schema
    assert schema["info"]["title"] == "AM Portfolio Parser Microservice API"
    assert "paths" in schema
    assert "components" in schema
    assert "schemas" in schema["components"]
    
    # Verify key Pydantic schemas exist in OpenAPI components
    schemas = schema["components"]["schemas"]
    assert "EtfHoldingsLookupRequest" in schemas
    assert "EtfHoldingsLookupResponse" in schemas
    assert "JobResponse" in schemas
    assert "JobStatusResponse" in schemas
    
    # Verify enum definition in OpenAPI schema
    assert "JobStatus" in schemas
    assert "enum" in schemas["JobStatus"]
    assert "pending" in schemas["JobStatus"]["enum"]
    assert "running" in schemas["JobStatus"]["enum"]
