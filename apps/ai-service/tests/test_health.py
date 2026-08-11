from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_liveness() -> None:
    response = client.get("/health/live")

    assert response.status_code == 200
    assert response.json() == {"status": "UP"}
    assert response.headers["X-Correlation-ID"]

    ready_response = client.get("/health/ready")
    assert ready_response.status_code == 200
    assert ready_response.json() == {"status": "UP"}


def test_replaces_unsafe_correlation_id() -> None:
    response = client.get("/health/live", headers={"X-Correlation-ID": "short"})

    assert response.headers["X-Correlation-ID"] != "short"


def test_structured_problem_does_not_leak_internal_exception() -> None:
    @app.get("/_test/unexpected")
    async def unexpected() -> None:
        raise RuntimeError("database-password-must-not-leak")

    safe_client = TestClient(app, raise_server_exceptions=False)
    response = safe_client.get(
        "/_test/unexpected", headers={"X-Correlation-ID": "request-12345678"}
    )

    assert response.status_code == 500
    assert response.headers["content-type"].startswith("application/problem+json")
    assert response.headers["X-Correlation-ID"] == "request-12345678"
    assert response.json()["code"] == "INTERNAL_ERROR"
    assert response.json()["correlationId"] == "request-12345678"
    assert "database-password" not in response.text
