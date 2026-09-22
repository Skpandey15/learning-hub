FROM ghcr.io/astral-sh/uv:0.12.17@sha256:10787c682e4184e4f290de1171fd4703dc63de99221f10fe1c99002ce7fa9acc AS uv

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
