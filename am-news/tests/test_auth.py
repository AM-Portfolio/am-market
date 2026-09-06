def test_user_routes_401_without_jwt(anon_client):
    assert anon_client.get("/v1/current-affairs").status_code == 401
    assert anon_client.post("/v1/insight", json={"symbols": []}).status_code == 401
    assert anon_client.get("/v1/holdings").status_code == 401


def test_user_cannot_start_feed(user_client):
    from am_news.api import deps
    from am_news.domain.entities import Principal

    user_client.app.dependency_overrides[deps.require_user] = lambda: Principal(
        subject="u1", roles=("user",)
    )
    user_client.app.dependency_overrides.pop(deps.require_admin, None)
    response = user_client.post("/v1/admin/feed/start")
    assert response.status_code == 403
    body = response.json()
    assert body["error_code"] == "FORBIDDEN"


def test_health_public(anon_client):
    assert anon_client.get("/health").status_code == 200
    assert anon_client.get("/ready").status_code == 200
