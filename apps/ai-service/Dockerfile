FROM python:3.15.0rc1-slim@sha256:858b8c9b2c764d1a3076e2c2ed64fb3f76ca2ec068ec81c1c059d3a5fcc1088e AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/app/.venv/bin:$PATH"

RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid 10001 --home /app app
WORKDIR /app
COPY --chown=app:app pyproject.toml uv.lock ./
RUN pip install --no-cache-dir uv==0.11.25 && uv sync --frozen --no-dev
COPY --chown=app:app app ./app
USER 10001:10001
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
