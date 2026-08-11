# Learning Hub V1 — Architecture

## 1. System context

Learning Hub is a greenfield modular application. The browser communicates only with the Spring Boot API. Spring Boot owns authentication decisions, catalog operations, content publication, and progress. FastAPI owns prompt execution and structured AI output. PostgreSQL is the source of truth.

```text
React Web UI
    │ OIDC + REST
    ▼
Spring Boot API ───────────────► PostgreSQL
    │ internal authenticated HTTP
    ▼
FastAPI AI Service ────────────► LiteLLM ────────────► OpenAI

Browser ───────► Zero-trust access layer ───────► Keycloak (login/token issuance)
Spring Boot ───► Keycloak (JWT validation)
```

## 2. Repository layout

```text
learning-hub/
├── apps/
│   ├── api/                 # Java 21 / Spring Boot / Gradle
│   ├── ai-service/          # Python / FastAPI
│   └── web/                 # React / TypeScript / Vite
├── platform/
│   ├── docker/
│   └── keycloak/
├── docs/
└── .github/workflows/
```

Use a single repository and a single PostgreSQL database in V1. Do not split catalog, content, and progress into separate deployable services.

## 3. Responsibilities

### React web

- Render catalog, lessons, and progress.
- Perform OIDC authorization-code flow with PKCE.
- Send access tokens to Spring Boot.
- Hold only transient UI state; do not store progress in local storage.
- Display controlled loading, empty, generation, and failure states.

### Spring Boot API

- Be the only public business API.
- Validate Keycloak JWTs and derive user identity from the token subject.
- Own taxonomy, study-content, generation-job, and progress transactions.
- Call FastAPI using an internal service credential.
- Validate AI responses again before persistence.
- Publish immutable content versions and select the current version.
- Calculate all aggregate progress.

### FastAPI AI service

- Build server-controlled prompts.
- Call LiteLLM with timeouts and bounded retries.
- Validate structured model output.
- Return content proposals; never write directly to PostgreSQL.
- Expose health/readiness endpoints and internal generation endpoints only.

### PostgreSQL

- Store curriculum taxonomy and ordering.
- Store immutable generated content versions and units.
- Store generation status and errors.
- Store per-user completion records.

### Keycloak

- Authenticate users.
- Issue JWTs with `candidate` or `interviewer` role claims.
- Manage local-development users through an imported realm configuration.

## 4. Core architectural rules

### Database-driven curriculum

React consumes catalog APIs. Slugs, ordering, skill levels, active flags, prerequisites, paths, and topic objectives live in PostgreSQL.

### AI creates content; deterministic services publish it

AI output is untrusted input. FastAPI performs schema validation; Spring Boot enforces length, topic identity, allowed unit types, uniqueness, and version rules. Only validated content is committed.

### Persist and reuse generated material

Generation is not repeated on every page view. A topic points to one current published content version. Regeneration creates a new immutable version; existing progress remains tied to stable unit identifiers within its version.

### Graceful degradation

Catalog, published lessons, and progress work without LiteLLM/OpenAI. Only new generation or regeneration is unavailable during an AI outage.

### Authorization and ownership

Permissions are resolved from token identity plus database-backed role-to-capability policy. The initial policy grants candidates and interviewers equal study rights, but administrators can change those grants without deployment. Explicit deny takes precedence, policy changes are audited, and authorization caches are invalidated immediately. A request body never supplies the authoritative user ID.

### Administrative control plane

`/admin` and `/api/v1/admin/**` form a separate control plane. They require the `admin` identity role plus the relevant application capability and step-up authentication for high-risk operations. Administrative writes use optimistic locking, validation, audit records, and explicit draft/publish/archive transitions. Draft catalog items never appear in learner APIs.

## 5. Backend modules

```text
com.learninghub
├── catalog
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── content
├── generation
├── progress
├── administration
├── authorization
├── security
└── shared
```

Dependencies flow from API to application to domain/persistence. Cross-module access goes through application services rather than repository sharing.

## 6. Content-generation lifecycle

```text
MISSING → QUEUED → GENERATING → VALIDATING → PUBLISHED
                    │               │
                    └──────► FAILED ◄┘
```

1. A topic page reports that content is missing.
2. An authorized user requests generation.
3. Spring Boot atomically creates a generation job and prevents duplicate active jobs.
4. A worker invokes FastAPI and records the result.
5. Spring Boot validates and transactionally writes a content version and its units.
6. The version becomes current and the job becomes `PUBLISHED`.
7. The UI polls the job or topic endpoint with bounded backoff.

V1 may run the worker inside the Spring Boot process with a bounded executor. The database job record makes the lifecycle observable and restart-safe. No message broker is required.

## 7. Failure semantics

- Unknown or inactive resources: `404`.
- Unauthenticated request: `401`.
- Insufficient role: `403`.
- Invalid request: `400`.
- Duplicate active generation: return existing job with `200`, or `409` if it cannot be reused.
- AI unavailable: generation job becomes `FAILED`; published content remains available.
- Rate limit exceeded: `429` with `Retry-After`.
- Unexpected errors: RFC 9457 problem response without secrets or model prompts.

## 8. Observability

Use structured logs and correlation IDs across Spring Boot, FastAPI, and LiteLLM. Capture:

- API latency and error count
- AI generation duration, success/failure, model, and token usage
- Generation queue depth and stale jobs
- Catalog and content cache hit rates, if caching is introduced
- Progress update count

Do not log tokens, credentials, full prompts, or private user data. Content bodies may be traced only through IDs and hashes.

## 9. Security and zero-trust baseline

- OIDC authorization code with PKCE for React.
- JWT issuer, audience, expiry, and role validation in Spring Boot.
- Internal FastAPI endpoint protected by a rotated service token or mTLS in later deployments.
- Secrets supplied through environment variables or a secret manager.
- Strict CORS allowlist.
- Request-size limits and output-size limits.
- HTML content is not accepted from the model; render structured plain text/Markdown through a sanitizer.
- AI prompts instruct against external instructions embedded in taxonomy data.
- Authenticate and authorize every request regardless of network location.
- Validate tokens at each service boundary; trust forwarded identity headers only from explicitly configured proxies that strip client-supplied copies.
- Prefer workload identity or short-lived credentials between services; static service tokens are local-development fallback only.
- Segment public, application, AI, identity, and data planes with default-deny ingress and egress.
- Require recent MFA/step-up authentication for access-policy, publication, and archive operations.
- Apply the controls in `ZERO_TRUST_SECURITY.md`.

## 10. Technology baseline

- Java current supported LTS, Spring Boot, Gradle, Spring Data JPA, Flyway
- PostgreSQL current stable major
- Python current stable feature release, FastAPI, Pydantic
- React current stable, TypeScript, Vite, React Router, TanStack Query
- Keycloak and LiteLLM exact stable versions
- JUnit/Testcontainers, Pytest, Vitest/Testing Library, Playwright for smoke tests

Exact verified versions and the upgrade policy live in `DEPENDENCY_BASELINE.md`. Floating production tags such as `latest` are prohibited.
