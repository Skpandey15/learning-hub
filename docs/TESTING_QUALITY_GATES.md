# Production Testing and Coverage Gates

## Policy

Coverage is a mandatory CI quality gate, not an informational metric. A pull request cannot pass CI if any application layer falls below its configured threshold. Tests must verify observable behavior; lowering a threshold or adding a coverage exclusion requires explicit review and a documented technical reason.

## Enforced gates

| Layer | Command | Enforced threshold | Report |
|---|---|---|---|
| Spring API | `./gradlew :apps:api:check --no-daemon` | Lines 95%, branches 95% | `apps/api/build/reports/jacoco/test/html/index.html` |
| FastAPI AI service | `uv run pytest` | Branch-aware aggregate 95% | `apps/ai-service/coverage.xml` |
| React web | `npm run test:coverage` | Lines, branches, functions, and statements 95% each | `apps/web/coverage/index.html` |

The React gate excludes test support, declaration files, and `main.tsx`, which is a side-effect-only browser bootstrap entry. Application behavior and error-boundary paths remain included.

## Verified baseline — 2026-08-11

| Layer | Result |
|---|---:|
| Spring API lines | 98.73% |
| Spring API branches | 100% |
| FastAPI aggregate | 98.10% |
| React lines, branches, functions, statements | 100% |

## CI and maintenance rules

- GitHub Actions executes the same commands used locally.
- Generated coverage reports are ignored by Git and must never be committed.
- Bug fixes require a regression test that fails without the fix.
- New error, authorization, validation, and security-sensitive paths require both success and failure-path tests.
- Coverage does not replace integration, contract, end-to-end, migration, security, or resilience testing.
- Any temporary quarantine must identify an owner, reason, expiry date, and tracking issue; quarantined tests cannot silently reduce these thresholds.
