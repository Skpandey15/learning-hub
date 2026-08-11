# Learning Hub V1 — Dependency Baseline

## Policy

Use the newest stable, mutually compatible releases available when implementation begins. Never use beta, release-candidate, nightly, canary, experimental, or floating `latest` artifacts in production. Pin exact versions in Gradle catalogs/wrappers, Python lockfiles, npm lockfiles, Docker image digests, and CI actions.

“Latest” is a bootstrap decision, not an unbounded runtime update policy. Renovate or Dependabot proposes updates; CI runs build, unit, integration, migration, security, and end-to-end tests before merge. Major upgrades require release-note review, compatibility proof, backup/rollback instructions, and staging soak.

## Verified baseline — 2026-08-11

| Component | Baseline | Selection note |
|---|---:|---|
| Java | 25 LTS | Current LTS; verify Spring/toolchain support at bootstrap |
| Spring Boot | 4.1.0 | Current stable line |
| Gradle wrapper | 9.6.1 | Current stable patch; verify Boot plugin matrix |
| Node.js | 24.18.1 LTS | Stable LTS patch compatible with the pinned Vite and jsdom toolchain |
| React / React DOM | 19.2.8 | Stable React patch pinned in lockfile |
| Vite | 8.2.1 | Current stable patch pinned in lockfile |
| Python | 3.14.6 | Current stable maintenance release |
| PostgreSQL | 18.4 | Current stable major/patch; PostgreSQL 19 is beta and excluded |
| Keycloak | 26.6.3 | Current stable security-patched release found during verification |
| FastAPI | 0.141.1 | Current stable package release |
| Pydantic Settings | 2.15.0 | Current stable package release |
| Uvicorn | 0.52.1 | Current stable package release |

FastAPI, Pydantic, LiteLLM, TypeScript, React Router, TanStack Query, Flyway, Testcontainers, Playwright, and other libraries must be resolved to their latest stable compatible patches during Phase 0 and recorded here by the implementation PR. Compatibility wins over a numerically newer incompatible release, but the deviation requires evidence and an upgrade issue.

## Compatibility gates

- Spring Boot plugin supports the selected Gradle and Java toolchain.
- Every Python dependency publishes/supports the selected Python version.
- Vite plugins support the selected React, Node LTS, and TypeScript versions.
- Keycloak adapter-free JWT validation works with the selected Spring Security line.
- PostgreSQL JDBC, Flyway, Testcontainers, and backup tooling support the selected database major.
- Docker images are multi-architecture where required and pinned by digest after validation.

## Production maintenance

- Security update SLA: critical 24 hours, high 7 days, medium within the normal release cycle.
- Monthly dependency review and automated update PRs.
- Quarterly major-version readiness review.
- Maintain an SBOM per image and preserve build provenance.
- Supported-version/EOL checks fail CI when a runtime is outside policy.
- Keep one tested rollback artifact and backward-compatible database migration path per release.
