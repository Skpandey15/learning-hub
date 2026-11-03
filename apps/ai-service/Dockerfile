FROM ghcr.io/astral-sh/uv:0.13.7@sha256:96f40d3904aad8d0bd010539001d36c8e2188b981e1334471a90c4a1d93bb490 AS uv

FROM python:3.14.6-slim@sha256:7bec7ddcddeff7975d6ba9b4be7dd6f6b2f55e7491539145e2978f7f97ce9144 AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/app/.venv/bin:$PATH"

RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid 10001 --home /app app
WORKDIR /app
COPY --from=uv /uv /uvx /bin/
COPY --chown=app:app pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev && /usr/local/bin/python -m pip uninstall --yes setuptools
COPY --chown=app:app app ./app
USER 10001:10001
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
