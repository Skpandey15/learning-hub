import re
import uuid
from collections.abc import Awaitable, Callable

from fastapi import FastAPI, Request, Response

CORRELATION_HEADER = "X-Correlation-ID"
SAFE_CORRELATION_ID = re.compile(r"^[A-Za-z0-9._-]{8,128}$")

app = FastAPI(
    title="Learning Hub AI Service",
    version="0.1.0",
    docs_url=None,
    redoc_url=None,
    openapi_url=None,
)


@app.middleware("http")
async def correlation_id(
    request: Request,
    call_next: Callable[[Request], Awaitable[Response]],
) -> Response:
    candidate = request.headers.get(CORRELATION_HEADER, "")
    request.state.correlation_id = (
        candidate if SAFE_CORRELATION_ID.fullmatch(candidate) else str(uuid.uuid4())
    )
    response = await call_next(request)
    response.headers[CORRELATION_HEADER] = request.state.correlation_id
    return response


@app.get("/health/live", include_in_schema=False)
async def live() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/health/ready", include_in_schema=False)
async def ready() -> dict[str, str]:
    return {"status": "UP"}
