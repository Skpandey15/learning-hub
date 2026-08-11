# Learning Hub V1 — Implementation Plan

## Mission

Build Learning Hub V1 as a new application from the documents in this directory. V1 delivers AI-generated study material and persistent learning progress; it does not include assessments or interview workflows.

## Delivery approach

Work in vertical, testable increments. Keep the main branch runnable. After each phase, run the smallest relevant test suite and record material deviations from these designs.

## Phase 0 — Bootstrap and guardrails

- Create the monorepo layout from `ARCHITECTURE.md`.
- Add `.editorconfig`, `.gitignore`, secret scanning, formatting, linting, and CI skeleton.
- Bootstrap Spring Boot/Gradle, FastAPI, and React/Vite.
- Add Dockerfiles and application health endpoints.
- Establish shared API-error and correlation-ID conventions.
- Resolve, compatibility-test, and exactly pin the stable versions in `DEPENDENCY_BASELINE.md`; generate initial SBOMs.

Exit criteria: every app builds and its unit-test command runs in CI.

## Phase 1 — Local platform and authentication

- Create PostgreSQL, Keycloak, LiteLLM, API, AI, and web Compose services.
- Import the development Keycloak realm.
- Implement React OIDC authorization code with PKCE.
- Configure Spring Security resource-server JWT validation.
- Map and test `candidate`, `interviewer`, and `admin` roles.
- Implement capability-based authorization and seed configurable grants.
- Add zero-trust trusted-proxy, token-audience, and authentication-context validation.
- Protect all learning APIs except liveness/readiness.

Exit criteria: both roles can sign in; anonymous API access returns `401`; Compose configuration validates.

## Phase 2 — Database and seeded curriculum

- Create Flyway migrations for all tables and indexes in `DATA_MODEL.md`.
- Seed eight domains, representative technologies, topic outlines, prerequisites, and learning paths.
- Create role, capability, grant, and append-only administrative audit tables.
- Add persistence integration tests using Testcontainers.
- Test clean installation, uniqueness constraints, and ordered queries.

Exit criteria: a clean database migrates and all seeded taxonomy can be queried without AI access.

## Phase 3 — Catalog API and web flow

- Implement catalog repositories, services, DTOs, and controllers.
- Return active items only and derive progress for the authenticated subject.
- Implement React API client and TanStack Query setup.
- Build `/learn`, domain, technology, topic-overview, and path pages.
- Add loading, empty, error, responsive, and accessibility states.

Exit criteria: users can navigate domain → technology → topic from database data on mobile and desktop.

## Phase 4 — AI generation service

- Define Pydantic input/output contracts.
- Implement versioned prompt templates and LiteLLM client.
- Add strict structured-output and semantic validation.
- Add service-token authentication, timeout, bounded retry, idempotency, and safe error mapping.
- Test model success, invalid output, repair, timeout, and provider failure using mocks.

Exit criteria: FastAPI returns a validated content proposal and never exposes provider credentials/errors.

## Phase 5 — Generation orchestration and publication

- Implement generation-job state machine and database uniqueness control.
- Implement a bounded background worker in Spring Boot.
- Add Spring Boot → FastAPI internal client with correlation ID and service token.
- Revalidate content, sanitize Markdown policy, create immutable versions and units, and publish transactionally.
- Add stale-job recovery, rate limits, and job status endpoints.
- Build missing/generating/failed/published topic UI states.

Exit criteria: one request generates and persists a lesson; duplicate requests do not duplicate jobs; published content remains readable with AI disabled.

## Phase 6 — Study reader and progress

- Implement topic-content endpoint with ETag.
- Build accessible unit reader, table of contents, code blocks, previous/next navigation, and AI notice.
- Implement access tracking and idempotent completion endpoint.
- Update unit and topic progress transactionally.
- Calculate technology, domain, and overall progress by unit count.
- Build `/progress` and continue-learning experience.
- Handle content-version changes explicitly.

Exit criteria: completion survives refresh, logout/login, and container restart; users cannot access another user's progress.

## Phase 7 — Hardening and observability

- Add structured logs, correlation IDs, metrics, and safe generation audit fields.
- Enforce request/output limits, strict CORS, rate limits, and concurrency caps.
- Run dependency, container, and secret scans.
- Verify Markdown sanitization and prompt-injection boundaries.
- Add database backup/restore notes and failed-job operational guidance.
- Enforce default-deny service networking, workload identity/mTLS compatibility, forwarded-header trust boundaries, and administrator step-up authentication.

## Phase 7A — Administration control plane

- Implement ecosystem/domain, technology, topic, path, and content-version administration APIs.
- Implement draft, preview, publish, archive, restore, ordering, validation, optimistic locking, and idempotency.
- Build `/admin` ecosystem hierarchy editor and content preview/publication workflow.
- Build role-capability policy matrix with effective diff, reason, step-up authentication, cache invalidation, and lockout prevention.
- Build read-only, indexed audit explorer.
- Test every capability, concurrent edit, policy change, audit record, and learner/admin boundary.

Exit criteria: an authorized admin can add and publish an arbitrary ecosystem without code changes; access-policy changes take effect promptly and every mutation is attributable.

Exit criteria: security tests pass and no secret or sensitive prompt appears in logs or Git history.

## Phase 8 — End-to-end validation and documentation

- Add Playwright smoke flows for both roles.
- Validate generation success and AI-outage degradation.
- Run backend, frontend, AI, integration, and accessibility suites.
- Test Docker Compose from a clean checkout and persistent restart.
- Document setup, environment variables, architecture, troubleshooting, and known V1 limitations.

Exit criteria: a new developer can start the platform from documented steps and complete a generated lesson.

## Test matrix

### Spring Boot

- Catalog filtering and ordering
- JWT role/subject mapping
- Ownership isolation
- Generation state transitions and duplicate suppression
- Publication transactions and version conflict handling
- Unit completion and aggregate progress
- RFC 9457 errors

### FastAPI

- Request and response validation
- Service authentication
- Prompt versioning
- LiteLLM success/failure behavior
- Semantic and Markdown validation
- Idempotency and timeouts

### React

- Catalog and topic rendering
- Authentication boundaries
- Generation states and polling
- Unit completion rollback on error
- Progress display
- Keyboard and accessible status behavior

### Integration/end-to-end

- Sign in as candidate and interviewer
- Sign in as admin, create and publish a new ecosystem, then verify learner visibility
- Change candidate/interviewer capability grants and verify allow/deny behavior
- Browse seeded taxonomy
- Generate and reopen persisted content
- Complete and undo a unit
- Resume after logout/login and restart
- Read existing content during AI outage
- Confirm cross-user progress isolation

## CI quality gates

- Formatting and linting
- Java, Python, and TypeScript unit tests
- Database integration tests
- Production builds
- Dependency and secret scanning
- Docker Compose configuration validation
- End-to-end smoke test on protected branches

## Implementation constraints

- Java 21 and constructor injection.
- PostgreSQL remains authoritative.
- React never calls FastAPI or LiteLLM directly.
- No user ID is trusted from request bodies.
- No progress is stored in local storage.
- Published content is immutable and versioned.
- No AI call occurs during ordinary content reads.
- No new infrastructure component is introduced without a demonstrated requirement.
- No assessment, quiz, scoring, or interview feature is added in V1.
- No credentials are committed.

## V1 acceptance checklist

- [ ] Greenfield monorepo builds in CI
- [ ] Docker Compose starts the integrated stack
- [ ] Candidate/interviewer study access is configurable and seeded equally
- [ ] Admin can manage and publish arbitrary ecosystems
- [ ] Admin can safely manage role-capability grants
- [ ] All admin mutations are audited
- [ ] Zero-trust boundary and forged-header tests pass
- [ ] All eight domains are seeded in PostgreSQL
- [ ] React taxonomy is API-driven
- [ ] Missing study material can be generated through FastAPI → LiteLLM → OpenAI
- [ ] Generated material is validated, versioned, and persisted
- [ ] Published lessons work when AI is unavailable
- [ ] Unit and aggregate progress persist
- [ ] Cross-user progress access is blocked
- [ ] Responsive and WCAG 2.2 AA basics are verified
- [ ] No assessments or interview features are present
- [ ] No secrets are committed or logged
- [ ] All automated tests pass
