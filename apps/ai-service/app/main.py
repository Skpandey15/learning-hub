import logging
import re
import time
import uuid
from collections.abc import Awaitable, Callable
from typing import Annotated

from fastapi import Depends, FastAPI, HTTPException, Request, Response
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.observability import configure_logging, correlation_id_context
from app.problems import ERROR_DEFINITIONS, AppError, ErrorCode, create_problem
from app.security import require_service_identity
from app.study_content import (
    GeneratedContent,
    GenerationRequest,
    StudyContentGenerator,
    get_generator,
)

CORRELATION_HEADER = "X-Correlation-ID"
SAFE_CORRELATION_ID = re.compile(r"^[A-Za-z0-9._-]{8,128}$")

configure_logging()
LOGGER = logging.getLogger(__name__)

app = FastAPI(
    title="Learning Hub AI Service",
    version="0.1.0",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)


@app.middleware("http")
async def request_observability(
    request: Request,
    call_next: Callable[[Request], Awaitable[Response]],
) -> Response:
    candidate = request.headers.get(CORRELATION_HEADER, "")
    correlation_id = candidate if SAFE_CORRELATION_ID.fullmatch(candidate) else str(uuid.uuid4())
    request.state.correlation_id = correlation_id
    context_token = correlation_id_context.set(correlation_id)
    started = time.perf_counter()
    response: Response | None = None
    try:
        response = await call_next(request)
        return response
    except Exception:
        LOGGER.exception(
            "Unhandled request exception",
            extra={"event.action": "unhandled_exception", "error.code": "INTERNAL_ERROR"},
        )
        response = _problem_response(request, ErrorCode.INTERNAL_ERROR)
        return response
    finally:
        duration_ms = round((time.perf_counter() - started) * 1000, 2)
        status_code = response.status_code if response is not None else 500
        if response is not None:
            response.headers[CORRELATION_HEADER] = correlation_id
        LOGGER.log(
            logging.ERROR if status_code >= 500 else logging.INFO,
            "HTTP request completed",
            extra={
                "event.action": "http_request",
                "http.request.method": request.method,
                "url.path": request.url.path,
                "http.response.status_code": status_code,
                "event.duration_ms": duration_ms,
            },
        )
        correlation_id_context.reset(context_token)


def _problem_response(
    request: Request,
    code: ErrorCode,
    *,
    violations: list[dict[str, str]] | None = None,
    headers: dict[str, str] | None = None,
) -> JSONResponse:
    correlation_id = getattr(request.state, "correlation_id", "unknown")
    problem = create_problem(
        code,
        instance=request.url.path,
        correlation_id=correlation_id,
        violations=violations,
    )
    return JSONResponse(
        status_code=problem.status,
        content=problem.model_dump(by_alias=True, exclude_none=True),
        media_type="application/problem+json",
        headers=headers,
    )


@app.exception_handler(AppError)
async def app_error_handler(request: Request, exception: AppError) -> JSONResponse:
    status_code = ERROR_DEFINITIONS[exception.code][0]
    LOGGER.log(
        logging.ERROR if status_code >= 500 else logging.WARNING,
        "Handled application error",
        exc_info=exception.__cause__,
        extra={"event.action": "api_error", "error.code": exception.code.value},
    )
    return _problem_response(request, exception.code)


@app.exception_handler(RequestValidationError)
async def validation_error_handler(
    request: Request, exception: RequestValidationError
) -> JSONResponse:
    violations = [
        {"field": ".".join(str(part) for part in error["loc"]), "message": error["msg"]}
        for error in exception.errors()
    ]
    LOGGER.warning(
        "Request validation failed",
        extra={
            "event.action": "request_validation_failed",
            "error.code": ErrorCode.VALIDATION_FAILED.value,
            "validation.fields": ",".join(violation["field"] for violation in violations),
            "validation.messages": ";".join(violation["message"] for violation in violations),
        },
    )
    return _problem_response(request, ErrorCode.VALIDATION_FAILED, violations=violations)


@app.exception_handler(HTTPException)
async def http_error_handler(request: Request, exception: HTTPException) -> JSONResponse:
    code = {
        400: ErrorCode.MALFORMED_REQUEST,
        401: ErrorCode.UNAUTHENTICATED,
        403: ErrorCode.ACCESS_DENIED,
        429: ErrorCode.RATE_LIMITED,
    }.get(exception.status_code, ErrorCode.INTERNAL_ERROR)
    headers = {"WWW-Authenticate": "Bearer"} if code == ErrorCode.UNAUTHENTICATED else None
    return _problem_response(request, code, headers=headers)


@app.exception_handler(Exception)
async def unexpected_error_handler(request: Request, exception: Exception) -> JSONResponse:
    LOGGER.exception(
        "Unhandled request exception",
        extra={"event.action": "unhandled_exception", "error.code": "INTERNAL_ERROR"},
    )
    return _problem_response(request, ErrorCode.INTERNAL_ERROR)


@app.get("/health/live", include_in_schema=False)
async def live() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/health/ready", include_in_schema=False)
async def ready() -> dict[str, str]:
    return {"status": "UP"}


@app.post(
    "/internal/v1/study-content/generate",
    dependencies=[Depends(require_service_identity)],
)
async def generate_study_content(
    request: GenerationRequest,
    generator: Annotated[StudyContentGenerator, Depends(get_generator)],
) -> GeneratedContent:
    return await generator.generate(request)
