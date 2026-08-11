import logging

import pytest
from fastapi import Depends, HTTPException
from fastapi.testclient import TestClient

from app.config import Settings, get_settings
from app.main import app
from app.observability import JsonFormatter, correlation_id_context
from app.problems import AppError, ErrorCode, create_problem
from app.security import require_service_identity


def settings() -> Settings:
    return Settings(
        internal_service_token="a" * 32,
        litellm_api_key="litellm-key",
    )


@app.get("/_test/protected", dependencies=[Depends(require_service_identity)])
async def protected() -> dict[str, bool]:
    return {"authorized": True}


@app.get("/_test/validation")
async def validation(value: int) -> dict[str, int]:
    return {"value": value}


@app.get("/_test/http/{status_code}")
async def http_error(status_code: int) -> None:
    raise HTTPException(status_code=status_code)


@app.get("/_test/app/{code}")
async def application_error(code: ErrorCode) -> None:
    raise AppError(code, cause=RuntimeError("provider-secret=hidden"))


@pytest.fixture(autouse=True)
def override_settings() -> None:
    app.dependency_overrides[get_settings] = settings
    yield
    app.dependency_overrides.clear()


def test_settings_can_be_loaded_and_cached(monkeypatch: pytest.MonkeyPatch) -> None:
    get_settings.cache_clear()
    monkeypatch.setenv("INTERNAL_SERVICE_TOKEN", "b" * 32)
    monkeypatch.setenv("LITELLM_API_KEY", "gateway-key")

    first = get_settings()
    second = get_settings()

    assert first is second
    assert first.internal_service_token == "b" * 32
    get_settings.cache_clear()


def test_service_identity_accepts_only_constant_time_matching_bearer() -> None:
    client = TestClient(app)

    missing = client.get("/_test/protected")
    invalid = client.get("/_test/protected", headers={"Authorization": "Bearer wrong"})
    valid = client.get("/_test/protected", headers={"Authorization": f"Bearer {'a' * 32}"})

    assert missing.status_code == 401
    assert invalid.status_code == 401
    assert valid.status_code == 200
    assert valid.json() == {"authorized": True}


def test_validation_and_http_errors_have_stable_problem_contract() -> None:
    client = TestClient(app)

    validation_response = client.get("/_test/validation", params={"value": "not-an-int"})
    assert validation_response.status_code == 400
    assert validation_response.json()["code"] == "VALIDATION_FAILED"
    assert validation_response.json()["violations"]

    expectations = {
        400: "MALFORMED_REQUEST",
        401: "UNAUTHENTICATED",
        403: "ACCESS_DENIED",
        429: "RATE_LIMITED",
        418: "INTERNAL_ERROR",
    }
    for status_code, code in expectations.items():
        response = client.get(f"/_test/http/{status_code}")
        expected_status = 500 if status_code == 418 else status_code
        assert response.status_code == expected_status
        assert response.json()["code"] == code


def test_expected_application_errors_are_safely_mapped() -> None:
    client = TestClient(app)

    for code in ErrorCode:
        response = client.get(f"/_test/app/{code.value}")
        assert response.json()["code"] == code.value
        assert "provider-secret" not in response.text


def test_problem_model_and_app_error_properties() -> None:
    cause = RuntimeError("hidden")
    error = AppError(ErrorCode.PROVIDER_TIMEOUT, safe_properties={"retry": True}, cause=cause)
    problem = create_problem(
        ErrorCode.PROVIDER_TIMEOUT,
        instance="/internal",
        correlation_id="request-12345678",
    )

    assert error.safe_properties == {"retry": True}
    assert error.__cause__ is cause
    assert problem.correlation_id == "request-12345678"
    assert problem.status == 503


def test_json_formatter_redacts_sensitive_values_and_handles_empty_exception_type() -> None:
    formatter = JsonFormatter()
    token = correlation_id_context.set("request-12345678")
    try:
        record = logging.LogRecord(
            "test",
            logging.ERROR,
            __file__,
            1,
            "token=plain-secret",
            (),
            (None, None, None),
        )
        formatted = formatter.format(record)
    finally:
        correlation_id_context.reset(token)

    assert "plain-secret" not in formatted
    assert "[REDACTED]" in formatted
    assert "request-12345678" in formatted
