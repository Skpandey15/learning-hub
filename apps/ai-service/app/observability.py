import contextvars
import json
import logging
import logging.config
import re
import traceback
from datetime import UTC, datetime
from typing import Any

correlation_id_context: contextvars.ContextVar[str] = contextvars.ContextVar(
    "correlation_id", default="unknown"
)

_RESERVED = set(logging.makeLogRecord({}).__dict__) | {"message", "asctime"}
_MAX_STACK_LENGTH = 8192
_SENSITIVE_VALUE = re.compile(
    r"(?i)\b(password|passwd|token|secret|authorization|api[_-]?key)\b([\s:=-]+)([^\s,;]+)"
)


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, Any] = {
            "@timestamp": datetime.fromtimestamp(record.created, UTC).isoformat(),
            "log.level": record.levelname,
            "log.logger": record.name,
            "service.name": "learning-hub-ai-service",
            "message": _redact(record.getMessage()),
            "correlationId": correlation_id_context.get(),
        }
        for key, value in record.__dict__.items():
            if key not in _RESERVED and _json_safe(value):
                payload[key] = _redact(value) if isinstance(value, str) else value
        if record.exc_info:
            stack = "".join(traceback.format_list(traceback.extract_tb(record.exc_info[2])))
            payload["error.stack_trace"] = _redact(stack[:_MAX_STACK_LENGTH])
            exception_type = record.exc_info[0]
            if exception_type is not None:
                payload["error.type"] = exception_type.__name__
        return json.dumps(payload, separators=(",", ":"), ensure_ascii=False, default=str)


def _json_safe(value: object) -> bool:
    return isinstance(value, (str, int, float, bool)) or value is None


def _redact(value: str) -> str:
    return _SENSITIVE_VALUE.sub(lambda match: f"{match.group(1)}=[REDACTED]", value)


def configure_logging() -> None:
    logging.config.dictConfig(
        {
            "version": 1,
            "disable_existing_loggers": False,
            "formatters": {"json": {"()": JsonFormatter}},
            "handlers": {
                "console": {
                    "class": "logging.StreamHandler",
                    "formatter": "json",
                    "stream": "ext://sys.stdout",
                }
            },
            "root": {"handlers": ["console"], "level": "INFO"},
            "loggers": {
                "uvicorn": {"handlers": ["console"], "level": "INFO", "propagate": False},
                "uvicorn.access": {"handlers": [], "level": "WARNING", "propagate": False},
            },
        }
    )
