# Implementation Status

## Phase 0 — Foundation

Status: complete on 2026-08-11.

Implemented:

- Greenfield Git monorepo and documented directory layout.
- Gradle 9.6.1 wrapper with Java 25 toolchain and Spring Boot 4.1.0 API module.
- Stateless OAuth2 resource-server baseline, safe error configuration, correlation IDs, health probes, metrics, Flyway, JPA, and PostgreSQL driver.
- Python 3.14 FastAPI service with strict settings, constant-time internal bearer validation foundation, safe correlation IDs, hidden public API schema, and health probes.
- React 19.2.8, TypeScript 6.0.3, Vite 8.2.1, React Router, TanStack Query, accessible application shell, linting, tests, and production build.
- Multi-stage non-root application containers.
- Compose topology for PostgreSQL 18.4, Keycloak 26.6.3, LiteLLM, API, AI service, and web application with segmented internal networks.
- Keycloak realm baseline with candidate, interviewer, and admin roles and SPA PKCE requirement.
- CI jobs for Java, Python, web, and Compose validation.
- Secret-safe sample environment configuration and repository ignores.

Verification:

- `./gradlew :apps:api:test`: passed.
- `uv run ruff check .`: passed.
- `uv run mypy app`: passed.
- `uv run pytest`: 2 passed; one upstream Starlette test-client deprecation warning remains.
- `npm run lint`: passed.
- `npm test`: 1 passed.
- `npm run build`: passed.
- `docker compose config --quiet`: passed.
- API, AI-service, and web container builds: passed.

Next phase:

- Authentication integration, JWT claim-to-role mapping, database-backed capabilities, zero-trust trusted-proxy controls, and authorization tests.
