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

## Production logging and exception handling

Status: implemented on 2026-08-11.

- ECS JSON logging, correlation IDs, bounded request events, and safe error fingerprints in Spring Boot.
- RFC problem responses for validation, malformed input, application, security, and unexpected errors.
- JSON logging, correlation-safe outer middleware, stable AI error taxonomy, redacted bounded frame output, and global fail-safe responses in FastAPI.
- Safe React render error boundary and query/header-free Nginx JSON access logs.
- Details and operational rules are documented in `OBSERVABILITY_ERROR_HANDLING.md`.

## Production test coverage gates

Status: implemented on 2026-08-11.

- The Spring API fails `check` below 95% line or branch coverage.
- The FastAPI service fails `pytest` below 95% branch-aware aggregate coverage.
- The React application fails `test:coverage` below 95% for lines, branches, functions, or statements.
- CI runs all three enforced gates on every pull request and every push to `main`.
- Commands, exclusions, report locations, and the verified baseline are documented in `TESTING_QUALITY_GATES.md`.

## Production CI/CD foundation

Status: implemented on 2026-08-11; external GitOps repository configuration remains an operator prerequisite.

- SHA-pinned CI with concurrency controls, timeouts, least-privilege permissions, retained test evidence, dependency review, and production-container contract builds.
- Scheduled and change-triggered CodeQL and Trivy security analysis.
- Immutable multi-architecture GHCR releases with digest scanning, BuildKit SBOM/provenance, GitHub attestations, and an attested release manifest.
- Automatic development GitOps dispatch and protected staging/production promotion using short-lived GitHub App tokens.
- Dependabot coverage for Actions, Gradle, Python, npm, and Docker dependencies.
- Required configuration and operational procedures are documented in `CICD_OPERATIONS.md`.
- Production Kubernetes Helm objects now cover API, AI service, web, Services, TLS Ingress, HPA, PDB, External Secrets integration, topology spreading, and NetworkPolicies.
- A GitOps receiver and automated Argo CD applications are provided for development, staging, and production; external cluster, DNS, certificate, secret-store, and repository bootstrap remain operator prerequisites.
