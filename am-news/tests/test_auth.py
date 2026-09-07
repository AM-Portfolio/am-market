import jwt

from am_news.api import deps
from am_news.domain.entities import Principal


def _hs256(roles: list[str]) -> str:
    return jwt.encode(
        {"sub": "qa", "iss": "https://auth.asrax.in/auth/realms/am-realm", "realm_access": {"roles": roles}},
        "test",
        algorithm="HS256",
    )


def test_user_routes_401_without_jwt(anon_client):
    assert anon_client.get("/v1/current-affairs").status_code == 401
    assert anon_client.post("/v1/insight", json={"symbols": []}).status_code == 401
    assert anon_client.get("/v1/holdings").status_code == 401


def test_user_cannot_start_feed(user_client):
    user_client.app.dependency_overrides[deps.require_user] = lambda: Principal(
        subject="u1", roles=("user",)
    )
    user_client.app.dependency_overrides.pop(deps.require_admin, None)
    response = user_client.post("/v1/admin/feed/start")
    assert response.status_code == 403
    body = response.json()
    assert body["error_code"] == "FORBIDDEN"


def test_admin_jwt_can_read_feed(anon_client):
    token = _hs256(["user", "admin"])
    response = anon_client.get("/v1/admin/feed", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 200


def test_user_jwt_cannot_read_feed(anon_client):
    token = _hs256(["user"])
    response = anon_client.get("/v1/admin/feed", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 403
    assert response.json()["error_code"] == "FORBIDDEN"


def test_health_public(anon_client):
    assert anon_client.get("/health").status_code == 200
    assert anon_client.get("/ready").status_code == 200
