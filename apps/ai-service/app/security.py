import hmac
from typing import Annotated

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import Settings, get_settings
from app.problems import AppError, ErrorCode

bearer = HTTPBearer(auto_error=False)


def require_service_identity(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> None:
    if credentials is None or not hmac.compare_digest(
        credentials.credentials.encode(), settings.internal_service_token.encode()
    ):
        raise AppError(ErrorCode.UNAUTHENTICATED)
