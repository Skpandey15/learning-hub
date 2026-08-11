from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_liveness() -> None:
    response = client.get("/health/live")

    assert response.status_code == 200
    assert response.json() == {"status": "UP"}
    assert response.headers["X-Correlation-ID"]


def test_replaces_unsafe_correlation_id() -> None:
    response = client.get("/health/live", headers={"X-Correlation-ID": "short"})

    assert response.headers["X-Correlation-ID"] != "short"
