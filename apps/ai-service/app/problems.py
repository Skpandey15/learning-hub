from enum import StrEnum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class ErrorCode(StrEnum):
    VALIDATION_FAILED = "VALIDATION_FAILED"
    MALFORMED_REQUEST = "MALFORMED_REQUEST"
    UNAUTHENTICATED = "UNAUTHENTICATED"
    ACCESS_DENIED = "ACCESS_DENIED"
    RATE_LIMITED = "RATE_LIMITED"
    PROVIDER_TIMEOUT = "PROVIDER_TIMEOUT"
    PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE"
    INVALID_MODEL_OUTPUT = "INVALID_MODEL_OUTPUT"
    INTERNAL_ERROR = "INTERNAL_ERROR"


ERROR_DEFINITIONS: dict[ErrorCode, tuple[int, str, str]] = {
    ErrorCode.VALIDATION_FAILED: (400, "Invalid request", "The request failed validation."),
    ErrorCode.MALFORMED_REQUEST: (400, "Malformed request", "The request could not be parsed."),
    ErrorCode.UNAUTHENTICATED: (
        401,
        "Authentication required",
        "Valid service authentication is required.",
    ),
    ErrorCode.ACCESS_DENIED: (
        403,
        "Access denied",
        "The caller is not permitted to perform this operation.",
    ),
    ErrorCode.RATE_LIMITED: (429, "Rate limit exceeded", "Too many requests were received."),
    ErrorCode.PROVIDER_TIMEOUT: (
        503,
        "Provider timeout",
        "The model provider did not respond in time.",
    ),
    ErrorCode.PROVIDER_UNAVAILABLE: (
        503,
        "Provider unavailable",
        "The model provider is temporarily unavailable.",
    ),
    ErrorCode.INVALID_MODEL_OUTPUT: (
        502,
        "Invalid model output",
        "The model returned an invalid response.",
    ),
    ErrorCode.INTERNAL_ERROR: (500, "Internal server error", "The request could not be completed."),
}


class ProblemResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", populate_by_name=True)

    type: str
    title: str
    status: int
    detail: str
    instance: str
    code: ErrorCode
    correlation_id: str = Field(alias="correlationId")
    violations: list[dict[str, str]] | None = None


class AppError(Exception):
    def __init__(
        self,
        code: ErrorCode,
        *,
        safe_properties: dict[str, Any] | None = None,
        cause: Exception | None = None,
    ) -> None:
        super().__init__(code.value)
        self.code = code
        self.safe_properties = safe_properties or {}
        self.__cause__ = cause


def create_problem(
    code: ErrorCode,
    *,
    instance: str,
    correlation_id: str,
    violations: list[dict[str, str]] | None = None,
) -> ProblemResponse:
    status, title, detail = ERROR_DEFINITIONS[code]
    return ProblemResponse(
        type=f"https://learninghub.dev/problems/{code.value.lower().replace('_', '-')}",
        title=title,
        status=status,
        detail=detail,
        instance=instance,
        code=code,
        correlation_id=correlation_id,
        violations=violations,
    )
